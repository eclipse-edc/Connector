# Extraction of service for presentation requests

## Decision

We will extract a service for requesting Verifiable Presentations from the `DcpIdentityService`. This new service
will be responsible for fetching the SI token and making the presentation request when verifying a received token.

## Rationale

Currently, the entire process for verifying a received token when using DCP is located in the `DcpIdentityService's`
`verifyJwtToken` method. This on the one hand causes the method to be quite long and on the other hand does not
allow for flexibility within the verification process. By extracting part of the process to a dedicated service, the
method can be shortened and adopters gain more flexibility to extend the code for requesting the presentation
with custom logic.

## Approach

### PresentationRequestService

We will create a new interface `PresentationRequestService` in the `decentralized-claims-spi`. This service will be
responsible for requesting the Verifiable Presentation after receiving the counter-party's SI token, including
the creation of the own SI token:

```java
public interface PresentationRequestService {
    Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                      String counterPartyDid, String counterPartySiToken,
                                                                      List<String> scopes);
}
```

The new interface will be used in the `DcpIdentityService's` `verifyJwtToken` method, where the calls to the
`SecureTokenService`, the `CredentialServiceUrlResolver` and the `CredentialServiceClient` will be replaced with a call
to the new service. The code replaced in the `DcpIdentityService` will be moved to the
`DefaultPresentationRequestService`:

```java
public class DefaultPresentationRequestService implements PresentationRequestService {
    // services & constructor

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                             String counterPartyDid, String counterPartySiToken,
                                                                             List<String> requestedScopes) {
        Map<String, Object> siTokenClaims = Map.of(PRESENTATION_TOKEN_CLAIM, counterPartyToken,
                ISSUED_AT, Instant.now().getEpochSecond(),
                AUDIENCE, counterPartyDid,
                ISSUER, ownDid,
                SUBJECT, ownDid,
                EXPIRATION_TIME, Instant.now().plus(5, ChronoUnit.MINUTES).getEpochSecond());
        var siToken = secureTokenService.createToken(participantContextId, siTokenClaims, null);
        if (siToken.failed()) {
            return siToken.mapFailure();
        }
        var siTokenString = siToken.getContent().getToken();

        return credentialServiceUrlResolver.resolve(counterPartyDid)
                .compose(url -> credentialServiceClient.requestPresentation(url, siTokenString, requestedScopes));
    }
}
```

### `decentralized-claims` lib module

As one of the goals of this refactoring is to allow adopters to extend the current logic for making the presentation
request, the `DefaultPresentationRequestService` should be available in a lib module. We will therefore create a new
lib module within the `decentralized-claims` module, which for now will only contain the
`DefaultPresentationRequestService`.
