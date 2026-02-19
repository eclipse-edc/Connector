# Dynamic JSON-LD Context Inference

## Decision

We will implement a mechanism to dynamically infer JSON-LD contexts based on the structure and content of the data being
processed.

## Rationale

Currently, EDC relies on predefined JSON-LD contexts to interpret and process data. Users can register
custom JSON-LD contexts to use when compacting JSON-LD data depending on the scope where the compaction is performed.
We have different scopes for DSP. DCP and Management API but once the context is registered
it's always used for all compaction operations within that scope. This has served well for connectors operating in a
single dataspace, but as we expand to support multiple dataspaces/participans in the same runtime, having static
JSON-LD context for single scopes becomes a limitation.

For example let's say we have two dataspaces X and Y, each with its own set of JSON-LD contexts for custom policy terms
definitions or custom asset terms. Currently, the only way to support this use case is to inject a `@context` of all the
supported dataspaces within a scope, but this can lead to conflicts and maintenance issues as the number of dataspaces
and custom contexts grows.

As an example let's say that:

- Dataspace X has a custom JSON-LD context : `https://w3id.org/X/context.jsonld`.
- Dataspace Y has a custom JSON-LD context: `https://w3id.org/Y/context.jsonld`.

And we want users to be able to define policies with custom terms from both dataspaces.

When processing a policy definition create or update is easy since users can include in the `@context` the
desired context for a dataspace. EDC will expand and store the policy definition in expanded form which will include
the IRIs for the custom terms compatible with the `@context` in input.

However, when we want to return the policy definition in a response to a user request or, transport that
policy over the DSP protocol, we need to compact the expanded form back to a more human-readable form.

With a static JSON-LD config for each scope:

```
jsonLd.registerContext("https://w3id.org/X/context.jsonld",MANAGEMENT_SCOPE_V4);
jsonLd.registerContext("https://w3id.org/Y/context.jsonld",MANAGEMENT_SCOPE_V4);
```

it means that the output will look like this:

```json
{
  "@context": [
    ...
    "https://w3id.org/X/context.jsonld",
    "https://w3id.org/Y/context.jsonld"
  ],
  ...
}
```

causing potential conflicts or causing the counter-party to not be able to properly interpret the data if it doesn't
support both contexts, while also making the output more verbose and harder to read.

## Approach

We will change the `JsonLd` service to support dynamic context inference. This will involve:

1. Allowing users to bind namespaces to JSON-LD contexts.
2. When compacting JSON-LD data, the service will analyze the data to determine which namespaces are present and infer
   the appropriate contexts to use for compaction based on the registered namespaces and their associated contexts.

```java

/**
 * Register a namespace binding. This will be used to generate the context during compaction.
 *
 * @param namespace  the namespace
 * @param contextIri the string representing the IRI where the context is located
 */
void registerNamespaceBinding(String namespace, String contextIri);
```

This way, when compacting data, the service can look at the namespaces used in the data and determine which contexts to
include in the `@context` of the output, allowing for more flexible and accurate compaction based on the actual data
being processed.

Additionally, we will provide a configuration based approach to allow users to specify the bindings between namespaces
and contexts, so that they can easily manage and update their context configurations without needing to change code.

As side note, this change is additive and backward compatible, since users can still register static contexts for
scopes.