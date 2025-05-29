# Dataspace Context

## Decision

We'll introduce the concept of "Dataspace context", that will permit a connector to being able to interact with multiple
dataspaces with potential different protocol versions, authentication and profiles 

## Rationale

At the moment the EDC connector only permits to support different protocol version at the same time, by having pluggable
modules that add support for them.

A Dataspace context is a more hi level concept that enables to define a set of components that will permit the connector
to interact also with different dataspaces, by defining a set of these capabilities:
- wire protocol, version and binding
- authentication protocol
- identifier
- scopes
- ...

## Approach

A Dataspace context will be bound to a single DSP endpoint.

Please note that will be possible to define multiple contexts for a single protocol version, there will be an api context
for every dataspace.

By default, there will be a single context registered for every DSP module (v0.8, 2024/1, 2025/1, ...) with default identity
service (DCP), default identifier (DID) and so on.

The adopter will then be able to override these ones with dataspace specific contexts with potentially different capabilities.
That could be done through configuration, under the `edc.dataspace.context` group.
If no setting is found, the current default `protocol` context will be used, otherwise:

```
edc.dataspace.context.<name>.protocol=dataspace-protocol-http:2025/1
edc.dataspace.context.<name>.auth=dcp:1.0
edc.dataspace.context.<name>...
```

Details about how to configure specific capabilities will come in the actual implementations.
