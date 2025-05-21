# Participant identity for DSP version

## Decision

We'll set up extension points to permit to use different DSP identifiers with different protocol versions.
This will also open up the possibility to manage different Dataspaces in the same connector.

## Rationale

At the moment the way the identifier is either put in DSP artifacts as Catalogs and Contract Agreements and the way it
is extracted from the counter-party credentials is hardcoded.
So if a Dataspace decides to chan the identifier starting from a specific DSP version, currently this is not
possible. 
In the long term vision about having the connector being able to manage multiple dataspace, and potentially multiple
identifiers this refactoring activity becomes mandatory.

## Approach

The changes will happen in 2 different contexts: ingress and egress.

### Ingress
When a new message is received on the DSP protocol the connector will have the possibility to decide how to extract the
id from the claim token based on the protocol version.

Currently this is done in the `ParticipantAgentService.createFor(ClaimToken token)` method, in which a list of registered
`ParticipantAgentServiceExtension` gets executed, returning a `Map<String, String>`. These attributes gets then added to
the `ParticipantAgent`, and, if one has the `edc:identity`, then it gets considered as the participant id:

```java
    @Override
    public ParticipantAgent createFor(ClaimToken token) {
        var attributes = new HashMap<String, String>();
        extensions.stream().map(extension -> extension.attributesFor(token)).forEach(attributes::putAll);
        if (!attributes.containsKey(PARTICIPANT_IDENTITY)) {
            var claim = token.getClaim(identityClaimKey);
            if (claim != null) {
                attributes.put(PARTICIPANT_IDENTITY, claim.toString());
            }
        }
        return new ParticipantAgent(token.getClaims(), attributes);
    }
```

This approach is in any case quite confusing, because the identifier is a mandatory property in a `ParticipantAgent`, but
now it's considered "nullable", while there are a lot of validations that are checking that identifier should not be null,
like:
- [`ValidatedConsumerOffer`](https://github.com/eclipse-edc/Connector/blob/3f59ef6db98153e96750d637d19d8141faeb7a95/spi/control-plane/contract-spi/src/main/java/org/eclipse/edc/connector/controlplane/contract/spi/validation/ValidatedConsumerOffer.java#L30)
- pretty much every method in [`ContractValidationService`](https://github.com/eclipse-edc/Connector/blob/3f59ef6db98153e96750d637d19d8141faeb7a95/core/control-plane/control-plane-contract/src/main/java/org/eclipse/edc/connector/controlplane/contract/validation/ContractValidationServiceImpl.java#L76-L77)
  validates this
- [`ContractValidationService` 2](https://github.com/eclipse-edc/Connector/blob/3f59ef6db98153e96750d637d19d8141faeb7a95/core/control-plane/control-plane-contract/src/main/java/org/eclipse/edc/connector/controlplane/contract/validation/ContractValidationServiceImpl.java#L90-L93)

So the proposal here is to: 
- make the `ParticipantAgent` have a non-null `id` field, if no id can be provided, the call must return an unauthorized
  as soon as possible, likely in the [`DspRequestHandler`](https://github.com/eclipse-edc/Connector/blob/3f59ef6db98153e96750d637d19d8141faeb7a95/data-protocols/dsp/dsp-core/dsp-http-core/src/main/java/org/eclipse/edc/protocol/dsp/http/message/DspRequestHandlerImpl.java)
- have the `ParticipantAgent` created in the `DspRequestHandler` before doing the service call, and the service call
  will receive the `ParticipantAgent` instance instead of the `TokenRepresentation` (that's only used to generate the 
  `ParticipantAgent` instance in any case).
- In the `ProtocolTokenValidator` there will be a `ParticipantAgentIdExtractorRegistry` that, based on the protocol version
  will call a specific `ParticipantAgentIdExtractor` with this signature:
  ```java
    String extractParticipantIdentity(ClaimToken token);
  ```
  by default, it will have the same implementation provided by `DefaultDcpParticipantAgentServiceExtension`, then adopters
  can register custom extractors for different versions.

### Egress
In the outbound communication there are two places in which the participant id is set: 
- [`ProviderContractNegotiationManager`](https://github.com/eclipse-edc/Connector/blob/f491d2fe83a69e113bdcb2f89b8c0f556e555ef9/core/control-plane/control-plane-contract/src/main/java/org/eclipse/edc/connector/controlplane/contract/negotiation/ProviderContractNegotiationManagerImpl.java#L137-L148)
  during the `ContractAgreement` creation, here the `ParticipantAgentIdExtractorRegistry` will be used again, by passing
- [`CatalogProtocolService`](https://github.com/eclipse-edc/Connector/blob/f491d2fe83a69e113bdcb2f89b8c0f556e555ef9/core/control-plane/control-plane-aggregate-services/src/main/java/org/eclipse/edc/connector/controlplane/services/catalog/CatalogProtocolServiceImpl.java#L64)

To permit the runtime to choose which id to set, a `ParticipantIdProvider` service will be introduced:
```java
interface ParticipantIdProvider {
    String participantId(String protocol);
}
```

The default implementation will return the `context.getParticipantId()` value, while adopters would be able to chose to
use different values for different protocol versions.

