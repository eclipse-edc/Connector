# Remove the `DispatcherRegistry`

## Decision

The `RemoteMessageDispatcherRegistry` and its single implementation `RemoteMessageDispatcherRegistryImpl` will be
removed from the codebase. Each messaging sub-system that currently routes through the registry will instead hold a
direct reference to its own dispatcher.

## Rationale

`RemoteMessageDispatcherRegistry` was introduced as a generic routing layer that, in theory, could have dispatched
messages across multiple protocols and transport variants. In practice, it never fulfilled that promise:

- **DSP (DPS)** is the only protocol that ever registered a dispatcher. The registry therefore acts as an indirection
  with a map of exactly one entry.
- **Callbacks** were also routed through the registry, but PR #5808 already replaced that path with a dedicated
  `CallbackClient`, eliminating the last non-DSP consumer.
- **DCP and other message-passing sub-systems** define their own dispatchers directly and never touched the registry.

The registry adds ceremony — callers must pass a `participantContextId` out-of-band, even when it is already embedded
in the message (as the callback case demonstrated, see #5658) — without providing any real abstraction benefit.
Removing it makes the call-graph simpler and easier to follow.

## Approach

### Components to remove

- `RemoteMessageDispatcherRegistry` SPI interface (`core-spi`)
- `RemoteMessageDispatcherRegistryImpl` (`runtime-core`)
- `RemoteMessageDispatcher` SPI interface (`core-spi`), replaced by direct calls to `DspHttpRemoteMessageDispatcher`
  where needed

### Protocol routing

The registry's only runtime behavior was selecting a dispatcher by `message.getProtocol()`. Because there is now a
single DSP dispatcher, this lookup becomes unnecessary.

Multiplexing across DSP versions (currently only `2025/1`, with future versions to follow) is already handled inside
`DspHttpRemoteMessageDispatcherImpl` through two collaborators:

- **`DspProtocolTypeTransformerRegistry`** — resolves the correct transformer registry for the outgoing message's
  protocol string, so each DSP version can produce a differently shaped JSON-LD payload.
- **`DataspaceProfileContextRegistry`** — resolves the active dataspace profile for the protocol, which
  `JsonLdRemoteMessageSerializerImpl` uses to pick the right JSON-LD compaction context.

Adding support for a new DSP version therefore only requires registering a new profile and its transformer set; no
change to the dispatcher or the protocol-routing layer is needed.
