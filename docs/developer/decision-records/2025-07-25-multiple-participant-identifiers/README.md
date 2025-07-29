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
add a new field `participantId` to the `DataspaceProfileContext`, which allows assigning a specific identifier to a
specific context. The `DataspaceProfileContextRegistry` will provide a new method for getting the participant ID for a
given protocol:

```java
@NotNull
String getParticipantId(String protocol);
```

The previously used participant ID will remain the default, i.e. it will be added as the participant ID to all default
profiles registered at the `DataspaceProfileContextRegistry`. If no dedicated participant ID is defined for a given
protocol, the method will return `null`:

```java
public String getParticipantId(String protocol) {
    return profiles().stream()
            .filter(it -> it.name().equals(protocol))
            .map(DataspaceProfileContext::participantId)
            .findAny()
            .orElse(null);
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

As ID extraction should depend on the `DataspaceProfileContext`, a function for extracting the participant ID from a
`ClaimToken` needs to be associated with a `DataspaceProfileContext`. For this function, we add a new interface as
shown below. A new field `idExtractionFunction` will be added to the `DataspaceProfileContext` of this type.

```java
@FunctionalInterface
@ExtensionPoint
public interface ParticipantIdExtractionFunction extends Function<ClaimToken, String> { }
```

Accordingly, we add another method to the `DataspaceProfileContextRegistry` for obtaining the ID extraction function
for a given profile context:

```java
ParticipantIdExtractionFunction getIdExtractionFunction(String protocol);
```

The `ProtocolTokenValidatorImpl` will obtain the ID extraction function from the `DataspaceProfileContextRegistry`,
apply it to the `ClaimToken` and pass the resulting ID to the `ParticipantAgentService`.

#### Default ID extraction function

There will be a default function for ID extraction that will be set for all default profiles. As the default behaviour
may differ depending on the `iam` extensions used, it will be obtained through the `ServiceExtensionContext` via a
second new interface:

```java
@FunctionalInterface
@ExtensionPoint
public interface DefaultParticipantIdExtractionFunction extends ParticipantIdExtractionFunction { }
```

A respective instance of `DefaultParticipantIdExtractionFunction` will be registered by the `iam` extensions, at the
moment namely `DCP` and `iam-mock`. For `iam-mock`, the default function will provide the current fallback behaviour
of looking for a configurable claim in the `ClaimToken`.

When using DCP, the logic for extracting the counter-party ID is currently defined in the
`DefaultDcpParticipantAgentServiceExtension`. This will be replaced with a `DefaultDcpIdExtractionFunction`
containing the same logic:

```java
public class DefaultDcpIdExtractionFunction implements DefaultParticipantIdExtractionFunction {
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
