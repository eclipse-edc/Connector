# Single-pass JSON-LD deserialization for incoming requests

## Decision

`AbstractJerseyJsonLdInterceptor` will be split into a write-only `WriterInterceptor` and a
globally-registered `MessageBodyReader<JsonObject>`. The reader will deserialize the incoming body,
validate it, and expand it in a single pass.

`@SchemaType` will gain a mandatory `version` attribute so that the globally-registered reader can
identify the schema version without per-resource interceptor configuration.

## Rationale

The current `ReaderInterceptor` implementation in `AbstractJerseyJsonLdInterceptor` deserializes the
request body twice:

1. `aroundReadFrom` deserializes the raw bytes into a `JsonObject`, expands it via JSON-LD, then
   re-serializes the result back to bytes and replaces the interceptor `InputStream`.
2. `context.proceed()` causes Jersey's built-in Jackson `MessageBodyReader` to deserialize those
   bytes a second time into the `JsonObject` the controller receives.

The re-serialization step exists only because `ReaderInterceptorContext` communicates through an
`InputStream`; there is no way to return a transformed object directly and still call
`context.proceed()` without skipping subsequent interceptors in the chain.

## Approach

### 1. Extend `@SchemaType` with a mandatory `version` attribute

```java
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchemaType {
    String[] value();
    String version();
}
```

This makes the schema version available to `MessageBodyReader.readFrom()` through its
`Annotation[] annotations` parameter, which already carries all parameter-level annotations of the
resource method. No new annotation type is needed.

The `version` attribute is mandatory: every `@SchemaType` usage must declare an explicit schema
version. Parameters on controllers that do not require validation must not carry `@SchemaType` at
all.

### 2. Replace `ReaderInterceptor` with `MessageBodyReader<JsonObject>`

A new class (e.g. `JsonObjectReader`) registered globally will:

1. Deserialize the bytes into a `JsonObject` via `TypeManager`.
2. If `@SchemaType` is present on the parameter and `version` is non-empty, validate the object
   against `JsonObjectValidatorRegistry`.
3. Return the JSON-LD-expanded `JsonObject` directly — no re-serialization, no `proceed()`.

Because this is a `MessageBodyReader`, Jersey will call it exactly once per request parameter and
return the result to the controller directly. The Jackson `MessageBodyReader` will not be invoked
for `JsonObject` parameters.

### 3. Strip the read side from `AbstractJerseyJsonLdInterceptor`

`AbstractJerseyJsonLdInterceptor` will implement only `WriterInterceptor`. The `validatorRegistry`
and `schemaVersion` constructor parameters will be removed. The write side (compaction logic)
is unchanged.

### 4. Annotate resource method parameters with `@SchemaType(value = "...", version = "v4")`

Every `JsonObject` parameter that currently has `@SchemaType` and whose corresponding interceptor
was constructed with a non-null `validatorRegistry` and `schemaVersion` will add the `version`
attribute.

Parameters on controllers whose interceptor was constructed without validation (DSP 2025 endpoints,
`DataPlaneSelectorApiExtension`) must not carry `@SchemaType` at all — the reader only validates
when the annotation is present.

### Consequences

- Request bodies are deserialized once instead of twice.
- `MessageBodyReader` can only be registered globally, not per resource class via `DynamicFeature`.
  This is not a constraint for the read path: JSON-LD expansion is context-agnostic (the JSON-LD
  context is embedded in or resolvable from the document itself), and validation is driven by the
  per-parameter `@SchemaType` annotation rather than by which interceptor instance was registered
  for a given controller.
- The write path will be tackled with a separate issue.

### Target modules

| Module | Change |
| --- | --- |
| `spi:core-spi` | Add `version` attribute to `@SchemaType`. |
| `core:common:lib:core-lib` | Add `JsonObjectReader implements MessageBodyReader<JsonObject>`; remove `ReaderInterceptor` from `AbstractJerseyJsonLdInterceptor`; remove `validatorRegistry`/`schemaVersion` fields and constructor parameters. |
| `extensions:common:http:jersey-core` | Register `JsonObjectReader` globally in `JerseyRestService` alongside `ObjectMapperProvider`. |
| All management API extensions | Add `version` attribute to `@SchemaType` annotations on `JsonObject` parameters where the corresponding interceptor was constructed with a non-null `schemaVersion`. |
