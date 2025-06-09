# Dataspace Context

## Decision

We'll introduce the concept of "Dataspace context", which will enable a connector runtime to serve requests for multiple
dataspaces with potentially different protocol versions, authentication, and profiles. 

## Rationale

At the moment the EDC connector only supports multiple protocol versions at the same time, by having pluggable
modules.

A Dataspace context is a higher-level concept that defines a configuration set and code required to service a request for a particular dataspace.
This includes:
- wire protocol, version and binding
- authentication protocol
- identifier
- scopes
- policy vocabulary and functions

## Approach

A Dataspace context will be bound to a single DSP endpoint.

By default, there will be a single dataspace context registered for every DSP module (v0.8, 2024/1, 2025/1, ...) with default identity
service (DCP), default identifier (DID) and so on.

The adopter will then be able to override these with dataspace specific contexts with potentially different capabilities.
That could be done through configuration, under the `edc.dataspace.context` group.
If no setting is found, the current default `protocol` context will be used, otherwise:

```
edc.dataspace.context.<name>.protocol=dataspace-protocol-http:2025/1
edc.dataspace.context.<name>.auth=dcp:1.0
edc.dataspace.context.<name>...
```

Details about how to configure specific capabilities will come in the actual implementations.
