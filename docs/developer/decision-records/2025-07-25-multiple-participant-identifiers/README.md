# Different participant identifiers for Dataspace Profile Contexts

## Decision

We will enable the connector to use different participant identifiers for different dataspace profile contexts.

## Rationale

The dataspace profile context concept outlined in [this decision record](../2025-05-28-dataspace-profile-context/README.md)
includes the support of identity resolution depending on the dataspace profile context. Right now, the connector uses
one fixed participant ID and always uses the same logic for extracting the counter-party ID when a message is received.
In order to support the usage of different identifiers depending on the context, the dataspace profile context needs
to be considered for both the resolution of the own participant ID and the extraction of the counter-party ID.

## Approach

> Note: throughout this DR, we will use the term `protocol` as the key for all operations utilizing the
> `DataspaceProfileRegistry` for backwards compatibility. This will be renamed to `profile` in the future.

### Resolution of own participant ID

Currently, the participant ID is determined through one setting and will be used all throughout the connector. We will
add a participant ID to the `DataspaceProfileContext` as an optional field, which allows assigning a specific identifier
to a specific context. The `DataspaceProfileContextRegistry` will provide a new method for getting the participant ID
for a given protocol:

```java
@NotNull
String getParticipantId(String protocol);
```

The previously used participant ID will remain the default and will be set in the `DataspaceProfileContextRegistry` as
such. Thus, if no dedicated participant ID is defined for the given protocol, the method will return the default ID:

```java
public String getParticipantId(String protocol) {
    return profiles().stream()
            .filter(it -> it.name().equals(protocol))
            .map(DataspaceProfileContext::participantId)
            .findAny()
            .orElse(defaultParticipantId);
}
```

In all places where the participant ID is used (namely during creation of a catalog or a contract agreement), an
instance of `DataspaceProfileContextRegistry` will be added, and the registry's `getParticipantId` method will be
used where currently the participant ID is inserted.

### Extraction of counter-party ID

Right now, extraction of the counter-party ID from a `ClaimToken` is done in the `ParticipantAgentServiceImpl`, either
through a `ParticipantAgentServiceExtension` or, as a fallback solution, by looking for a specific claim that can be
defined through configuration.

The extraction will be pulled up into the `ProtocolTokenValidatorImpl` to separate extraction of the ID from the
extraction of any other participant attributes, which will still be handled by the `ParticipantAgentServiceExtensions`.
The extracted ID will then be passed to the `ParticipantAgentService` as a parameter.

As ID extraction should depend on the `DataspaceProfileContext`, an additional method will be added to the
`DataspaceProfileContextRegistry` for registering a new profile. In addition to the profile itself, this method will
also accept a function for extracting the participant ID from a `ClaimToken`, an instance of
`Function<ClaimToken, String>`:

```java
void register(DataspaceProfileContext context, Function<ClaimToken, String> idExtractionFunction);
```

Accordingly, another method will be added to obtain the ID extraction function for a given profile context:

```java
@NotNull
Function<ClaimToken, String> getIdExtractionFunction(String protocol);
```

All registered profiles that were created without passing a function for ID extraction will use the default function.
This will be supplied during creation of the `DataspaceProfileContextRegistryImpl` and contain the current default
behaviour of looking for a configurable claim in the `ClaimToken`. As the default logic may be different depending on
the `iam` extension used (e.g. for DCP the credential subject ID from received VCs is used), the default function must
be overridable. Therefore, a third method will be added to the `DataspaceProfileContextRegistry`: 

```java
void setDefaultIdExtractionFunction(Function<ClaimToken, String> extractionFunction);
```

The `ProtocolTokenValidatorImpl` will obtain the ID extraction function from the `DataspaceProfileContextRegistry`,
apply it to the `ClaimToken` and pass the resulting ID to the `ParticipantAgentService`.

#### ID extraction for DCP

When using DCP, the logic for extracting the counter-party ID is currently defined in the
`DefaultDcpParticipantAgentServiceExtension`. This will be replaced with a `DefaultDcpIdentityExtractionFunction`
containing the same logic:

```java
public class DcpIdentityExtractionFunction implements Function<ClaimToken, String> {
    @Override
    public String apply(ClaimToken claimToken) {
        return ofNullable(claimToken.getListClaim(CLAIMTOKEN_VC_KEY)).orElse(emptyList())
                .stream()
                .filter(o -> o instanceof VerifiableCredential)
                .map(o -> (VerifiableCredential) o)
                .flatMap(vc -> vc.getCredentialSubject().stream())
                .map(CredentialSubject::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
```

This function will be set as the default ID extraction function on the `DataspaceProfileContextRegistry` by the
`IdentityAndTrustExtension`, which previously registered the `ParticipantAgentServiceExtension`.
