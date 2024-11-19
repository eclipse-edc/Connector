# Transformers versioning scheme

## Decision

We will remove the thrown exception when calling `TypeTransformerRegistry#forContext` on nested TypeTransformerRegistry
for supporting incremental versioning scheme.

## Rationale

The current approach for versioning is to create a new `TypeTransformerRegistry` instance for each context + version.
e.g. in dsp context:

```java
var dspApiTransformerRegistryV08 = transformerRegistry.forContext("dsp-api:v0.8");
var dspApiTransformerRegistryV2024_1 = transformerRegistry.forContext("dsp-api:2024/1");
```

This versioning scheme is well suited for the dsp context where message serialization/deserialization may have substantial changes
between versions and where versions don't have a direct relationship between them.

This approach might not be the best for the management-api context for example, where we usually apply incremental
changes in the form of alpha APIs introducing new fields or changing the shape of existing fields in limited entities,
while preserving the existing fields and their shape in others.

## Approach

By removing the exception when calling `TypeTransformerRegistry#forContext` on a nested, it will be possible to
create specific versioned context containing only the override transformers.

```java
var mgmtContext = transformerRegistry.forContext("management-api");
var mgmtContextV4Alpha = mgmtContext.forContext("V4Alpha");
// override mgmtContext transformer
mgmtContextV4Alpha.registerTransformer();
```