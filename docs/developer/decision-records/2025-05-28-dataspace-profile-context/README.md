# Dataspace Profile Context

## Decision

We'll introduce the concept of "Dataspace profile context", which will enable a connector runtime to serve requests for multiple
dataspaces with potentially different protocol versions, authentication, vocabulary and policies. 

## Rationale

At the moment the EDC connector only supports multiple protocol versions at the same time, by having pluggable
modules.

The Dataspace Profile Ccontext is an higher-level concept that defines a set of:
- wire protocol, version and binding
- authentication protocol
- policy functions
- vocabulary (jsonld context)
- scopes
- identifier resolution

## Approach

A `DataspaceProfileContextRegistry` service will be in place, on which a `DataspaceProfileContext` can be registered.

By default, there will be an hardcoded context that uses a particular combination of DSP, DSP, vocabulary and so on; such
version will be useful for testing and samples, but for a production use the adopters must explicitly define and register
the expected dataspace contexts by extension.

So, for example, if a connector is supposed to support multiple DSP version, there will be the need to explicitly register
one profile for every version, same will happen with multiple DCP version and so on.

Every profile will be represented by a different protocol endpoint, and it will be advertised in the `.well-known/dspace.version`
endpoint.

### Additional Notes
To be backward compatible, the `profile` information will be passed to management-api and stored as `protocol`, so out of
the box the default profile name will be the DSP version. In the future this could be adapted and the `protocol` attribute/field
could be renamed to `profile`, but there's no urgency to do that.

To permit certain assets to be published only on certain profiles the `assetsSelector` field of the `ContractDefinition`
can be used, no plans to have a strict correlation between Assets and Dataspace Profile Contexts at the moment.
