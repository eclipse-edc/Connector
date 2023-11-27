# Protocol Services Refactor

## Decision

Decouple the DSP (or other) protocol from the identity service and move the security checks (`IdentityService#verifyJwtToken`) 
to the protocol service layer.

## Rationale

Implementations of `IdentityService`s may need additional context/request information when verifying the JWT token. At the
DSP (or other protocols) layer we don't have such information. Moving the security checks on the protocol services layer will 
allow us to attach contextual information to a specific request (e.g. current policy if any).

## Approach

We will remove the usage of `IdentityService` from the `DspRequestHandlerImpl` and change the `serviceCall` field in `DspRequest`

from:

```java
BiFunction<I, ClaimToken, ServiceResult<R>> serviceCall;
```
to:
```java
BiFunction<I, TokenRepresentation, ServiceResult<R>> serviceCall;
```

This will impact each method of the three protocol service we have now:

- `CatalogProtocolService`
- `TransferProcessProtocolService`
- `ContractNegotiationProtocolService`

In each implementation of such services, we'd have to call then the `IdentityService` for verifying the JWT token. 