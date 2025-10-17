# Participant Context Configuration

## Decision

We will implement a configuration system for participant contexts that allows for flexible and dynamic management of
participant context.

## Rationale

Currently, EDC configuration needs to be provided at boot time. This makes it hard to manage configuration properties
that are related to a participant context. Since we are introducing the concept of a participant context, we need a way
to provide and manage participant context specific configurations.

This will allow dynamic provisioning of participant contexts and their configurations without requiring a restart.

## Approach

We will introduce three new SPI interfaces:

- `ParticipantContextConfig`
- `ParticipantContextConfigStore`
- `ParticipantContextConfigService`

The `ParticipantContextConfig` will be the participant context aware `Config` variant, and it will
provide methods to get configuration properties for a specific participant context:

```java
public interface ParticipantContextConfig {

    // Retrieve configuration value for a specific participant context and key
    String getString(String participantContextId, String key);
    
    ... // Additional methods for other data types
}
```

This will be an injectable component that can be use in extensions for loading participant context specific
configuration at runtime.

The `ParticipantContextConfigStore`  will store and retrieve participant context configurations.

```java
public interface ParticipantContextConfigStore {

    void save(String participantContextId, Config config);

    Config get(String participantContextId);
}
```

This will be an `@ExtensionPoint` point with multiple implementations, e.g. in-memory, database, etc.
We will provide a shim layer when running in a single participant context mode.

The `ParticipantContextConfigService` will provide higher level operations for managing participant context
configurations.

```java
public interface ParticipantContextConfigService {

    ServiceResult<Void> save(String participantContextId, Config config);

    ServiceResult<Config> get(String participantContextId);
}
```

The `ParticipantContextConfigService` will provide a higher level API for managing participant context configurations.
It provides transactional boundaries and validation, and it could be used in a REST API for managing participant context
configurations.

#### Validation

Currently, the validation is done at boot time. We will need to introduce a validation mechanism that can be used
at runtime when saving participant context configurations. This could be done by reusing the existing validation
mechanism of `Validator` and `ValidatorResult` as we do for `JsonObject`.