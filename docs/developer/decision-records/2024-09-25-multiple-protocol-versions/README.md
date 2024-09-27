# Multiple protocol versions

## Decision

We will introduce a `protocolVersion` concept that will be carried in the current `protocol` property, that will drive
the selection the right protocol version for connector to connector communication.

The protocol convention will be `<protocolName>:<protocolVersion>`

## Rationale

Currently, EDC supports only [DSP](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol)
in the current stable form. As the spec is evolving, we should prepare the EDC for supporting multiple versions in the
same EDC runtime. This will enable the communication between two EDC connectors as long as they agree on a common
supported version.

## Approach

Since we are enriching the `protocol` property with a version, no additional changes is required in `RemoteMessage`s,
`TransferProcess` and `ContractNegotiation`.

We will refactor `RemoteMessageDispatcherRegistry` to:

```java
public interface RemoteMessageDispatcherRegistry {
    /**
     * Registers a dispatcher.
     */
    void register(String protocol, RemoteMessageDispatcher dispatcher);
}
```

and we will remove the `RemoteMessageDispatcher#protocol` method.

This will allow us to register a `RemoteMessageDispatcher` for multiples protocols, which means for example that in the
context of [DSP](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol)
we can register the `DspHttpRemoteMessageDispatcher` for multiple DSP versions while keeping the service injectable.

In the context of [DSP](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol) implementation
a protocol version consists of:

- A set of Transformers for `RemoteMessage`s serialization/deserialization.
- A scoped JSON-LD `@context` configured in the `JsonLd` service.
- A set of Controllers for receiving protocol `RemoteMessage`s.

We should provide BOMs for protocol versions and any common logic between versions should be extracted in `*-libs`.

### Transformers

Instead of having a single `TypeTransformerRegistry` for the `dsp-api` context, we should have one for
each [DSP](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol) supported version.

### JSON-LD `@context`

Currently, we bind [DSP](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol) specific
namespaces/contexts definition under the scope `DSP`. We should have multiple configurations for each supported version

### Controllers

We already provide versioned controllers, but each version should have its own `JerseyJsonLdInterceptor` for injecting
the right JSON-LD scope. This can be achieved using `jakarta.ws.rs.container.DynamicFeature` interface.

The `protocol` + `version` should be added also in `DspRequest` for selecting the right
`RemoteMessage`s transformers.

## Further considerations

We should expose in the Management context an API for fetching the
supported [versions](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol/common-functionalities/common.protocol)
of a `counterParty`

We might apply in the future a way to automatically negotiate a common supported version if no version is specified.

