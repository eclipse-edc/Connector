# Contract Negotiation Approval

## Decision

We will support approving provider-side contract negotiations before they move on to agreement, without custom Java
code:

- a provider negotiation in `REQUESTED` is held (`pending`) when its contract policy does not pass evaluation in a new
  `approval.contract.negotiation` policy scope;
- a new `contract.negotiation.held` event is published, and it can be delivered through the existing callback
  mechanism, which is extended with per-participant callbacks;
- two Management API endpoints approve or reject a held negotiation.

To make this possible next to custom guards, multiple `ContractNegotiationPendingGuard`s can be active at the same
time.

## Rationale

The [state machine guards](../2023-07-20-state-machine-guards) introduced the `pending` flag to let a state machine stop
and wait for an external interaction. However:

- the only way to hold an entity is to provide a custom `*PendingGuard` Java extension, and only one guard can be active
  at a time;
- the Management API commands for acting on pending entities, which that decision record expected, were never built.
  Nothing in the connector clears the `pending` flag, so adopters have to write their own resume mechanism.

Approving negotiations manually before they reach an agreement is the most common reason for a guard. It should work
out of the box. The policy engine, with Java functions or [CEL expressions](../2026-01-27-adopt-cel-expressions),
already
lets adopters express rules such as "negotiations from counterparties outside the `trusted` partner group need
approval". It is therefore the natural place to make the decision.

A general, dynamically configurable pending-guard framework for every entity and state was considered and dismissed as
too broad for the use cases at hand.

## Approach

### The approval decision

A new policy scope `approval.contract.negotiation` is introduced with a context class
`ApprovalContractNegotiationPolicyContext`, which extends `ContractNegotiationPolicyContext`.

When a provider negotiation is in `REQUESTED`, the contract policy stored on the negotiation (the policy of the last
contract offer) is evaluated in this scope:

- the evaluation succeeds: the negotiation is auto-approved and proceeds as today;
- the evaluation fails: the negotiation is held for manual approval.

The scope name deliberately does not start with `contract.negotiation`. Rule bindings match scopes by prefix, so a name
like `contract.negotiation.approval` would inherit every constraint bound to `contract.negotiation`. Those constraints
would then be evaluated a second time, without the counterparty's claims. With `approval.contract.negotiation`, which
follows the pattern of `request.contract.negotiation`:

- only constraints explicitly bound to the approval scope are evaluated, either with `bindScope` (Java) or with
  `CelExpression.scopes` (CEL);
- every other constraint is filtered out before evaluation. A contract policy without approval constraints therefore
  evaluates to `true`, so existing deployments keep auto-approving every negotiation;
- the `odrl:use` action is bound to the new scope, for the core and for CEL, so that permissions are not filtered out.

The policy engine selects functions by context class. Since the approval context extends
`ContractNegotiationPolicyContext`, the functions registered for negotiations can be used for approval constraints too,
including CEL with `ctx.agent` and `ctx.partners`.

The counterparty is represented by a `ParticipantAgent` rebuilt from the negotiation's `counterPartyId`. Claims are not
stored on a contract negotiation, so approval constraints can rely on the identity, for example:

```
ctx.partners.byIdentity(ctx.agent.id).inGroup('trusted')
```

but not on the counterparty's credentials. Storing the claims on the negotiation is a possible follow-up.

Because approval constraints live in the contract policy, they are visible to the consumer in the catalog and in the
agreement, like any other constraint of that policy.

### Pending guard

A `ContractNegotiationApprovalGuard` matches provider negotiations in `REQUESTED` whose approval evaluation fails. The
state machine already handles a matching guard: it sets `pending` and stops picking the negotiation up.

Today exactly one `ContractNegotiationPendingGuard` can be provided, and the default one never matches. To let the
approval guard coexist with custom guards:

- a `ContractNegotiationPendingGuardRegistry` is introduced. Guards are registered in it, and it matches when any of the
  registered guards matches;
- the managers (and the task executor) use the injected guard combined with the registry using a logical `or`. A guard
  provided through `@Provider` keeps working;
- providing a single `ContractNegotiationPendingGuard` is deprecated in favour of registering guards in the registry;
- the approval guard is registered in the registry by the core.

### Notification

A new `ContractNegotiationHeld` event (`contract.negotiation.held`) is published whenever a negotiation is set
pending by a guard, through a new `held` method on `ContractNegotiationListener`. It is not limited to approval: it
fires for every guard.

The event can be delivered by the existing `CallbackEventDispatcher`:

- **static callbacks** (`edc.callback.<name>.*`) work without changes, but they are global to the runtime;
- **dynamic callbacks** are attached to the negotiation by the consumer when it starts the negotiation. Provider
  negotiations are created from protocol messages and have none.

Instead of attaching callbacks to provider negotiations, the dispatcher is extended with **participant-scoped
callbacks**:

- a `ParticipantCallbackResolver` returns the callbacks of a participant context for an event;
- the default implementation reads them from the participant context configuration entry `edc.callbacks`. Its value is
  a JSON array with the same shape as a callback address: `uri`, `events`, `transactional`, `authKey` and `authCodeId`.
  If the entry is missing, the participant has no callbacks;
- for every event that carries a participant context id, the dispatcher sends the event to the static callbacks, the
  participant's callbacks and the event's own dynamic callbacks;
- callback secrets (`authCodeId`) are already resolved from the participant's vault partition.

This is generic: any participant can subscribe to any event. Since nothing is stored on the entities, configuration
changes apply immediately. A provider that wants to be notified about negotiations waiting for approval configures:

```json
[
  {
    "uri": "https://approvals.example.com/hook",
    "events": [
      "contract.negotiation.held"
    ],
    "transactional": false
  }
]
```

### Approve and reject

Two endpoints are added to the v5 contract negotiation API:

| Endpoint                                                                         | Body                                    |
|----------------------------------------------------------------------------------|-----------------------------------------|
| `POST /v5/participants/{participantContextId}/contractnegotiations/{id}/approve` | none                                    |
| `POST /v5/participants/{participantContextId}/contractnegotiations/{id}/reject`  | `RejectNegotiation {reason}` (optional) |

They require the `management-api:negotiations:write` scope and are authorized like the other negotiation endpoints.

They are implemented as commands (`ApproveNegotiationCommand`, `RejectNegotiationCommand`) executed through the command
handler registry, like `terminate`. Both commands only apply to a negotiation that is of type `PROVIDER`, in state
`REQUESTED` and `pending`. Any other negotiation results in a `409 Conflict`.

- **Approve** transitions the negotiation to `AGREEING` and clears `pending`. This is exactly what the state machine
  does for `REQUESTED`. Moving the negotiation out of `REQUESTED` also guarantees the guard is not evaluated again, so
  no approval needs to be recorded.
- **Reject** transitions the negotiation to `TERMINATING` with the given reason and clears `pending`. The termination
  message sent to the consumer carries the reason.

Negotiations waiting for approval can be listed with the existing query endpoint, filtering on `pending = true`.

### Testing

- **Unit tests:**
    - the approval guard: failing, passing and approval-free policies; consumer negotiations; other states;
    - guard registry composition;
    - the approve and reject command handlers, including the conflict cases;
    - the pending event and listener;
    - the participant callback resolver and the dispatcher.
- **End-to-end test:** a contract policy with a failing approval constraint holds the provider negotiation in
  `REQUESTED`, and a participant-scoped callback receives `contract.negotiation.held`. After `approve`, the negotiation
  reaches `FINALIZED`. After `reject`, it reaches `TERMINATED`.

### Out of scope

- **Task-executor mode.** The task executor skips the agreement task when a guard matches without setting `pending`.
  Supporting approval there means setting `pending` and having approve schedule the agreement task.
- **Counterparty claims on `ContractNegotiation`.** They would enable approval constraints based on credentials.
- **Other approvals:** consumer-side negotiations, transfer processes, and other states.
