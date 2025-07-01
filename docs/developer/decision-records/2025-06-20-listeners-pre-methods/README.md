# Listeners "pre" methods

## Decision

We'll deprecate and remove the `pre*` methods in the `ContractNegotiationListener` and `TransferProcessListener`

## Rationale

These methods were introduced in a time in which the EDC ecosystem was not yet well shaped to permit adopters to implement
additional missing logic before persistence.
But this extensibility in fact is no more necessary, as the entity should be shaped by the services that are provided out
of the box. 
The methods that don't start with `pre*` will be kept, as they are the backbone of the eventing system. 

## Approach

The `pre*` methods in listeners will be deprecated first and deleted after 2 versions.
