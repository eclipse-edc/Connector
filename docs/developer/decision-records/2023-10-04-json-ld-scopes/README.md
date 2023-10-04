# Json-Ld Scopes

## Decision

Implement Json-Ld scopes, which are runtime visibility boundaries that enable Json-Ld contexts to be specified for
different processing contexts. For example, Json-Ld processing for Dataspace Protocol messages and identity-trust
protocol messages will utilize different Json-Ld contexts. It must be possible to individually specify these contexts.

## Rationale

Currently, Json-Ld processing is handled by `JerseyJsonLdInterceptor` and `JsonLdRemoteMessageSerializerImpl` that are
registered for each ingess/egress context. On egress, during compaction, `JerseyJsonLdInterceptor`
and `JsonLdRemoteMessageSerializerImpl` delegate to `TitaniumJsonLd`, which synthesizes a Json-Ld context based on
namespaces registered using `JsonLd.registerNamespace(String prefix, String contextIri)`. This results in `@context`
structures containing extraneous information. For example, a registered `DCAT` namespace will be included in the context
synthesized for identity and trust endpoints, which is never referenced in messages.

Json-Ld scopes will provide a mechanism to avoid extraneous context items by making it possible to limit registration to
an endpoint context.

## Approach

### Namespace Registration

The `JsonLd` interface will be extended with an overloaded method containing a `scope` parameter:

```
void registerNamespace(String prefix, String contextIri, String scope)
```

The scope parameter will identify a Web Context Alias set in the `JerseyWebService`. A `*` value is treated as a
wildcard to indicate all web context aliases.

The existing `registerNamespace(String prefix, String contextIri)` method will be treated as a `*` wildcard
registration. This will maintain backward compatibility.

Existing registrations will be ported according to the following table:

| Namespace     | Web Context Alias         |
|---------------|---------------------------|
| EDC_NAMESPACE | *                         |
| DCAT_SCHEMA   | DSP                       |
| ODRL_SCHEMA   | DSP, MANAGEMENT_API, IATP |
| DSPACE_SCHEMA | DSP                       |

### Context Registration

The `JsonLd` interface will be extended to support registration of context references. For example:

```json
{
  "@context": [
    "https://example.org/context/v1"
  ]
}
```

The following methods will be added to `JsonLd`:

```
 void registerContext(String contextIri)
 
 void registerContext(String contextIri, String scope)
```

The first method variant will register a context reference using the `*` wildcard scope. On compaction, the `JsonLd`
service will insert references to registered contexts in the synthesized `@context`structure.

### Json-Ld Compaction

Json-Ld compaction will be performed in an egress scope that corresponds to the `JerseyWebService` context alias.
The `JsonLd` interface will be extended with an overloaded method containing a `scope` parameter:

```
Result<JsonObject> compact(JsonObject json, String scope);
```

The existing `Json.ld.compact(JsonObject json)` method will be treated as a `*` wildcard.

Constructors for `JerseyJsonLdInterceptor` and `JsonLdRemoteMessageSerializerImpl` will be updated to include a `scope`
parameter that will be used for compaction operations.

