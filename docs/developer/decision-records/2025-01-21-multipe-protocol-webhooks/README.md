# Multiple Protocol Webhooks

## Decision

We will add capabilities of registering multiple `ProtocolWebhook` for driving the selection
of the `callbackAddress` based on the chosen protocol.

## Rationale

Currently, the `ProtocolWebhook` is a single injectable that is used to determine the `callbackAddress`
to use for a remote protocol request. This is a limitation in cases where multiple protocols or
multiple versions of the same protocol are supported.

## Approach

We will introduce a registry for `ProtocolWebhook`s:

```java
public interface ProtocolWebhookRegistry {

    void registerWebhook(String protocol, ProtocolWebhook webhook);

    ProtocolWebhook resolve(String protocol);
}
```

where each protocol or each protocol version can register its own `ProtocolWebhook`.

The `ProtocolWebhookRegistry` will be injected in replacement of the single `ProtocolWebhook` and
it will be resolved at runtime based on the user provided protocol.

For the current DSP implementation we will register a `ProtocolWebhook` for each
DSP protocol version supported.

### Changes to the Catalog

Currently, the `ProtocolWebhook` is also used to configure the `endpointUrl` of the `DataService`
registered for the connector. Since we might have multiple `ProtocolWebhook` registered, we will
change the registration and resolution of `DataService`s based on the input protocol.