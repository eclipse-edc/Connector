# Participant Context (EDC Virtual)

## Decision

We will enable the connector to run workloads for multiple participants in the same runtime (EDC Virtual).

## Rationale

Currently, the connector is designed to run workloads for a single participant in a single runtime. For certain
deployment scenarios and use cases, this can be a limitation when scaling factor the connector deployment for multiple
participants, as it requires multiple instances of the connector to be deployed and managed.

## Approach

We will introduce a new concept and entity called "participant context", similar to the one we have implemented in
Identity Hub, that allows the connector to run workloads for multiple participants in the same runtime. This will enable
the connector to scale horizontally and improve its performance for certain use cases.

The current Connector repository, which contains the core modules for a connector, will only provide the foundation
in the core libraries and components but, it will only support a running single participant workload.
The implementation of multiple participant workloads will be done using connector core modules but in a separate
repository within the EDC organization.

### Entities

#### ParticipantContext

A new entity called `ParticipantContext` that represents the context of a participant in the connector will be
introduced:

```java
public record ParticipantContext(String participantContextId,...additionalProperties) {
}
```

It will contain information about the participant, such as an identifier, and other relevant details.
It might also carry the association between
a [dataspace profile context](../2025-05-28-dataspace-profile-context/README.md)
and the participant ID within that dataspace profile context.

To manage the participant contexts, we will add a store spi `ParticipantContextStore` and a service spi
`ParticipantContextService` with default implementations.

> The management APIs for participant context is out of the scope of this DR, and it will be implemented in EDC virtual
> repository.

#### Refactoring

The following entities will be refactored to carry the `participantContextId` property:

- `Asset`
- `PolicyDefinition`
- `ContractDefinition`
- `ContractNegotiation`
- `ContractAgreement`
- `TransferProcess`
- `DataPlaneInstance`
- `EndpointDataReferenceEntry`

The store and service interface for those entities won't change.

### Management APIs

The current implementation of the management APIs for backward compatibility will not support the `participantContextId`
in input in any form. We will provide a shim layer that infer the `participantContextId` from configuration (e.g
`edc.participant.id`) to associate each entity with a participant context.

### DSP APIs

Same as the management APIs, the current implementation of the DSP APIs for will not support the `participantContextId`
in input, and the participant context will be inferred from the configuration and passed to
the [protocol service layer](#protocol-service-layer).

### Policy Engine

To support a single participant operating in different
dataspaces ([dataspace profile context](../2025-05-28-dataspace-profile-context/README.md)), the policy engine should
have the capability to register and evaluate policy functions within a dataspace profile context, with bundled
association for default dataspace profile context (backward compatibility).

### Identity Service

The `IdentityService` will be refactored to be participant context aware.
Implementors of `IdentityService` may need to fetch additional information based on the participant context for minting
and validating auth tokens.

### Protocol Service Layer

The three protocol service interfaces:

- `CatalogProtocolService`
- `ContractNegotiationProtocolService`
- `TransferProcessProtocolService`

will be refactored to take the participant context as input. This will allow the protocol services to correlate and
verify a DSP request with the right participant context.

> Some refactoring will be required in order to forward the participant context to additional services used in the
> current protocol service layer implementation.

### Remote Message Dispatcher

The `RemoteDispatcher` interface will get a `participantContextId` as input.

An implementation of the `RemoteMessageDispatcher` may need the `participantContextId` to dispatch a message on behalf
of the right participant context. This is required for example in the `DspHttpRemoteMessageDispatcherImpl` class where a
call to the `IdentityService` is made to mint an auth token.

### Configuration

Configurations that are relevant for a participant should be refactored
to support single and multiple participant contexts deployments. This could be achieved case by case introducing an
interface for loading the configuration based on the participant context. The abstraction will allow for example loading
configuration properties as we do today for a single participant context and from a different storage (like database)
for multiple participant contexts.

Examples of settings that are relevant for a participant are:

- `edc.iam.issuer.id`
- `edc.iam.sts.oauth.*`

### Migration

The classic EDC will only support a single participant context, and users have
to configure and apply migrations to the EDC schema in order to support the new participant context aware entities.

When starting from scratch a new EDC instance the default participant context id will be taken from configuration
(e.g. `edc.participant.id`), but it's recommended to use a specific configuration `edc.participant.context.id` to avoid
confusion and assign a randomly generated UUID/String as participant context id.

When migrating an existing EDC instance to a participant context aware EDC instance, users also should provide a
migration script that updates all the existing entities to associate them with the default participant context id taken
from configuration (e.g. `edc.participant.context.id`).

It's important that the value set in configuration for the participant context id
is stable and doesn't change between restarts, and matches the one used in the migration script.

Since the value of the participant context id is arbitrary, hardcoding a specific value in the migration script is not
recommended.
If users are providing a distribution of the EDC, they should provide a
migration script that uses that specific configured value and apply it during the migration process.

For example when using Flyway this can be done with
migration [placeholders](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/migration-placeholders).
