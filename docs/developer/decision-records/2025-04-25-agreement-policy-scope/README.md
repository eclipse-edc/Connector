# Contract Agreement policy scope

## Decision

We will add a `contract.agreement` policy scope, that will be evaluated by the provider before the automatic agreement.

## Rationale

The negotiation flow is at the moment an automatic one: a negotiation can happen if the `contract.negotiation` policy 
scope evaluation succeeds, or it gets terminated if the evaluation fails.

To implement "manual negotiation approval", the `PendingGuard` service can be used, but this doesn't permit to leverage
on the flexibility and extensibility of the policy engine. 

## Approach

There will be a new policy scope/context to permit the new `PolicyEngine` evaluation, the class will look like:

```java
public class ContractAgreementPolicyContext extends PolicyContextImpl {

    @PolicyScope
    public static final String AGREEMENT_SCOPE = "contract.agreement";

    @Override
    public String scope() {
        return AGREEMENT_SCOPE;
    }
}
```

An additional evaluation of the `PolicyEngine` will be added in the `ProviderContractNegotiationManager` in the
`processRequested` method, so that every negotiation that gets `REQUESTED` can be pass through policy functions:
```java
    @WithSpan
    private boolean processRequested(ContractNegotiation negotiation) {
        var evaluation = policyEngine.evaluate(negotiation.getLastContractOffer().getPolicy(), new ContractAgreementPolicyContext());
        if (evaluation.succeeded()) {
            transitionToAgreeing(negotiation);
        } else {
            negotiation.setPending(true);
            observable.invokeForEach(l -> l.manualInteractionRequired());
            update(negotiation);
        }
    }
```

By doing this in the case the evaluation goes through the behavior will be the same (automatic agreement), otherwise the
negotiation will be put as "pending", that's the flag that prevent it to be taken into consideration from the state
machine.
The `observable` call will be used to generate an event, so that the client could get notified about the negotiation
waiting for manual interaction.


Possible follow up for this proposal would be endpoints and commands to approve or reject the negotiation, but they are
not part of this proposal.
