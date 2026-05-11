# Multi-Profile Support in EDC Virtual

## Decision

We will let a single virtual EDC runtime serve multiple dataspace profiles concurrently, on a
per-participant basis. Each profile carries its own dataspace identity (JSON-LD namespace and
context), is addressable through its own DSP URL segment, and is selected at request time based on
the participant context.

## Rationale

The [Dataspace Profile Context](../2025-05-28-dataspace-profile-context/README.md) introduced the
concept of a profile as the binding of wire protocol, version, vocabulary and identifier
resolution. The [Participant Context (EDC Virtual)](../2025-08-26-participant-context/README.md)
made it possible to run workloads for multiple participants in the same runtime.

Combining the two requires extending the profile model in three ways:

- A participant living in two dataspaces today shares JSON-LD namespace and `@context` URLs across
  both, because those values were sourced globally at boot. They must move into the profile.
- DSP HTTP endpoints must be addressable per profile (in addition to per participant), so a single
  runtime can host overlapping dataspaces without collisions on the protocol path.
- Components wired at boot (message dispatchers, JSON-LD interceptor, scopes) need to be selected
  per request based on the profile carried in the URL, instead of being bound globally.

## Approach

### Reshape of `DataspaceProfileContext`

`DataspaceProfileContext` gains two fields that capture the dataspace identity of the profile:

```java
public record DataspaceProfileContext(String name,
                                      ProtocolVersion protocolVersion,
                                      ProtocolWebhook webhook,
                                      ParticipantIdExtractionFunction idExtractionFunction,
                                      JsonLdNamespace protocolNamespace,
                                      List<String> jsonLdContextsUrl) {
}
```

The `name` is the profile id; it appears as a path segment in DSP URLs in virtual mode and as the
protocol string (`{profileId}`) instead of the current format `dataspace-protocol-http:{version}`. Lookup by id
(`registry.getProfile(profileId)`) uses the bare profile id, not a protocol-prefixed key.

A default profile is registered at boot time to preserve the existing single-profile behavior, with the same protocol
version and JSON-LD with name `http-dsp-profile-2025-1`.

### Registration callbacks on the registry

`DataspaceProfileContextRegistry` exposes `addRegistrationCallback(Consumer<DataspaceProfileContext>)`.
Subscribers are invoked once per already-registered profile when they subscribe, and again for
every subsequent registration. Components attach profile-scoped side effects through this
mechanism — JSON-LD context registration, dispatcher binding, policy-scope keying — without the
registry depending on those subsystems.

### `ParticipantProfileResolver` (new SPI)

A new SPI `ParticipantProfileResolver` resolves which profiles a participant is associated with.
It backs onto a per-participant configuration entry (`edc.dsp.profiles`, a comma-separated list of
profile ids) read through `ParticipantContextConfig`. It exposes two operations:

- `resolveAll(participantContextId)` — every profile the participant is associated with, in
  configured order; used by the version discovery endpoint to filter the response per participant.
- `resolve(participantContextId, profileId)` — single-profile lookup used by DSP controllers to
  validate that an inbound URL targets a profile the participant is entitled to.

Ids configured for a participant but not registered in the registry are skipped silently.

### Virtual DSP controllers

A new module group, `data-protocols/dsp/dsp-virtual/dsp-2025-virtual/`, exposes DSP endpoints with
participant and profile as path segments:

```
/{participantContextId}/{profileId}/catalog/request
/{participantContextId}/{profileId}/negotiations/...
/{participantContextId}/{profileId}/transfers/...
```

Each controller resolves the pair through `ParticipantProfileResolver.resolve(...)` and rejects
the request if no matching profile is associated with the participant. The classic (non-virtual)
controllers under `dsp-2025` remain unchanged for single-participant deployments.

### Profile-aware JSON-LD compaction

The existing Jersey JSON-LD interceptor is refactored into a shared base
(`AbstractJerseyJsonLdInterceptor`), with a new `ProfileJerseyJsonLdInterceptor` that derives the
JSON-LD scope from the `{profileId}` URL segment captured by a new `UrlInfoRequestFilter`. As a
result, each profile compacts and expands against its own `@context` document, even when several
profiles are served on the same runtime.

### Dispatcher binding per profile

`DspHttpDispatcherV2025Extension` no longer registers a single dispatcher at boot. Instead it
subscribes to the profile registry callback and registers a dispatcher per registered profile
whose protocol version matches `2025-1`, keyed by the profile id. This keeps the dispatcher
selection logic consistent with the inbound routing model.

### Discovery endpoint filtering

The `.well-known/dspace.version` (metadata) endpoint reports only the profiles that
`ParticipantProfileResolver` returns for the requested participant context, so version discovery
is participant-scoped in virtual mode.

### Backward compatibility

The classic single-participant runtime continues to register exactly one default profile, the
resolver returns it for the lone participant, and existing DSP URLs are unaffected. Adopters that
already register one standard profile keep the existing behaviour and only need to opt in to the
virtual controllers and the per-participant `edc.dsp.profiles` configuration to take advantage of
multi-profile support.

### Additional Notes

- Profile names must be unique within a runtime: they are used both as URL segments and as
  protocol-string suffixes.
- Per-participant profile association is read through
  [Participant Context Config](../2025-10-15-participant-context-config/README.md); the storage
  backend (in-memory or SQL) is a separate concern and is not constrained by this decision.
- This decision does not change how profiles are advertised on the dataspace side: profile ids
  remain stable strings agreed between participants.
