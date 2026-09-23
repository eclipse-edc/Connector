# Partners and Partner Groups

## Decision

We will introduce two participant-context-scoped entities, `Partner` and `PartnerGroup`, with stores, services and a
v5 Management API, and make them available to policy evaluation in Java (through `PartnerService`) and in CEL
(through a `ctx.partners` handle and a set of helper functions).

## Rationale

Adopters keep re-implementing "who is this counterparty to me" in custom policy functions: business identifiers,
tiers, allow-lists. The connector has no first-class notion of a partner known to a participant context, nor of groups
of partners, so such rules cannot be expressed without Java code, and in the virtual (multi-tenant) connector they
cannot be scoped per tenant at all.

## Approach

### Model

- `PartnerGroup { id, participantContextId, name, description, properties }`
- `Partner { id, participantContextId, identity, name, properties, groupIds }`

Both extend `AbstractParticipantResource`. A partner is identified by `(participantContextId, id)`; the same id may
exist in different participant contexts. Within a participant context the `identity` (the counterparty id the dataspace
profile extracts from the verified token, e.g. a DID) is unique as well. Membership is recorded on the partner as a set
of group ids; the service rejects references to groups that do not exist in the same participant context and refuses
to delete a group that is still referenced (`409`).

`properties` is a free-form map. Business identifiers such as a customer number live there and are queryable with nested criteria
(`properties.businessId = BID-0001`); group membership is queryable with `groupIds contains <groupId>`. These are the two
operators supported on both the in-memory and the SQL store.

The SQL tables `edc_partner` and `edc_partner_group` use a composite primary key `(participant_context_id, id)`; the
partner table adds a unique constraint on `(participant_context_id, identity)`.

### Resolution at evaluation time

Partners are resolved when a policy is evaluated, not when the `ParticipantAgent` is created. Core ships the data and a
query surface, not a matcher: whether a counterparty is matched by identity, by a business identifier extracted from a
credential, or by anything else is decided by the policy author.

To scope lookups, policy contexts now carry the participant context they are evaluated for through the new
`ParticipantContextPolicyContext` mixin. `CatalogPolicyContext` and `ContractNegotiationPolicyContext` take the id as
an additional constructor argument (the previous constructors are deprecated), `TransferProcessPolicyContext` and
`PolicyMonitorContext` derive it from the agreement.

`PolicyMonitorContext` additionally implements `ParticipantAgentPolicyContext`: the counterparty agent is rebuilt from
the agreement (identity = consumer id, claims = the claims snapshotted when the agreement was reached, no attributes).
This makes `ctx.agent` and `ctx.partners` uniformly available in the `catalog`, `contract.negotiation`,
`transfer.process` and `policy.monitor` scopes, so removing a partner from a group also terminates running transfers.

### Java

Custom policy functions inject `PartnerService` and use `findById`, `findByIdentity` or `search` with the participant
context id from the policy context. No built-in Java policy function ships in core.

### CEL

The CEL context gains a `partners` handle, `{ "participantContextId": "..." }`, added to every context that implements
the mixin. The `partner-cel` module registers member functions on it:

| Expression                                  | Result                                             |
|---------------------------------------------|----------------------------------------------------|
| `ctx.partners.byIdentity(s)`                | partner map                                        |
| `ctx.partners.byId(s)`                      | partner map                                        |
| `ctx.partners.query({'businessId': 'X'})`          | list of partner maps (property equality, AND)      |
| `p.inGroup(s)`                              | `bool`                                             |
| `p.groups()` / `p.groups`                   | list of group ids                                  |
| `p.found()`                                 | `bool`                                             |

A partner map has the shape `{ id, identity, name, properties, groups, found }`. Lookups never return `null`: an
unknown partner is a map with `found == false` and empty groups, so `ctx.partners.byIdentity(ctx.agent.id).inGroup('gold')`
evaluates to `false` for unknown counterparties instead of aborting the evaluation. `query` results are capped at 1000.

The functions are registered only when a `CelFunctionRegistry` is available, so CEL stays optional.

### Management API

Only the multi-tenant v5 API is provided:

- `/v5/participants/{participantContextId}/partners`
- `/v5/participants/{participantContextId}/partnergroups`

each with `POST`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` and `POST /request`. The scopes are
`management-api:partners:read` and `management-api:partners:write`.
