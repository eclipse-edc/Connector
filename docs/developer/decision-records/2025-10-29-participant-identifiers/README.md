# Participants Identifiers resolution

## Decision

We will introduce a participant identifier resolution mechanism that supports participant contexts and dataspace profile
context.

## Rationale

Currently, the resolution of a participant identifier is outlined
in [this decision record](../2025-07-25-multiple-participant-identifiers/README.md).

The current resolution mechanism allows to resolve a participant identifier based on the dataspace profile context.
But that mechanism is limited to a single identifier per context. We need to support a resolution mechanism
that allows to resolve identifiers for different participant contexts.

## Approach

We will remove the `getParticipantId` method from the `DataspaceProfileContextRegistry` and the `participantId` from
`DataspaceProfileContext`.
We will introduce a new interface `ParticipantIdentifierResolver`:

```java
public interface ParticipantIdentityResolver {

    @Nullable
    String getParticipantId(String participantContextId, String protocol);
}
```

We will inject this interface into all places where the participant ID is needed, e.g. during creation of a catalog
or a contract agreement.

By default, we will provide an implementation that return the participant ID defined in the config for single context
setup e.g. `edc.participant.id` which applies to every protocol.

```java
    // by default, resolve to the configured participant id for every protocol
@Provider(isDefault = true)
public ParticipantIdentityResolver participantIdentityResolver() {

    // retrieve participant id from config `edc.participant.id`
    return (context, protocol) -> participantId;
}
```

For supporting multiple identifiers based on the protocol instead of registering a DataspaceProfileContext with a
participant ID, a custom implementation of the `ParticipantIdentityResolver` should be provided that resolves the
participant ID based on the protocol (the participant context ID can be ignored in that case).

Additionally, we will remove ` ServiceExtensionContext#getParticipantId()` as well as any usage of it in the codebase.

