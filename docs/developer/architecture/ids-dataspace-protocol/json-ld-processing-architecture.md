# JSON-LD Processing Architecture

The Dataspace Protocol requires all protocol message types to be serialized as JSON-LD. The IDS REST binding specifications further define how those serialized message types are bound to
endpoints over HTTPS. Controller endpoints will be added to the EDC that support marshalling and unmarshalling JSON-LD messages. These controllers will operate on native JSON
structures (since JSON-LD is valid JSON) as opposed to typed Java representations. This will allow the EDC to remove its dependency on the IDS InfoModel Java Library and to provide
full support for JSON-LD message exchanges.

A common JSON-LD processing architecture is required by these controller endpoints, which may be deployed to individual runtimes such as the control plane and catalog systems. In
addition, the Management API will adopt support for JSON-LD. This document lays out the common processing architecture that will support those requirements.

## JSON-LD Support

JSON-LD support can be added to the existing `TypeManager` infrastructure by creating a __serializer context__ that is configured with JSONP support in a JSON-LD EDC extension:

```
var mapper = new ObjectMapper();

mapper.registerModule(new JSONPModule());

var module = new SimpleModule() {

    @Override
    public void setupModule(SetupContext context){
        super.setupModule(context);
    }
    
};

mapper.registerModule(module);

typeManager.registerContext("json-ld",mapper)
```

This context can then be used in conjunction with the [Titanium JSON-LD Library](https://github.com/filip26/titanium-json-ld) when processing messages:

```
// message is de-serialized as Map<String, Object> by Jersey 
var document = JsonDocument.of(mapper.convertValue(message, JsonObject.class));

try {

    var compacted = JsonLd.compact(document,EMPTY_CONTEXT).get();
    var convertedDocument = mapper.convertValue(compacted,Map.class);
    
    // process converted document

} catch(JsonLdError e) {
    throw new RuntimeException(e);
}
```

### JSON-LD Document Loaders

JSON-LD allows references to external documents that contain context information. EDC should provide a default loader and extensibility point for resolving and caching external
document contexts, `JsonLdDocumentResolverRegistry`. This extensibility point should be provided by a generic JSON-LD extension, as it may be reused outside of IDS.

The Titanium interface for loading referenced documents is `DocumentLoader`:

``` 
public interface DocumentLoader {

    /**
     * Retrieve a remote document.
     *
     * @param url of the remote document to fetch
     * @param options to set the behavior of the loader
     * @return {@link Document} representing a remote document
     * @throws JsonLdError
     */
    Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError;
}

```

EDC will provide an implementation that:

1. Resolves documents from a local file system directory (which can be a read-only docker mount)
2. Delegates to `JsonLdDocumentResolver` implementations via the `JsonLdDocumentResolverRegistry`.

`JsonLdDocumentResolver` should not expose Titanium classes. It should return a `JsonStructure` which can be wrapped in a Titanium `Document` by the EDC `DocumentLoader`
implementation:

```
public interface JsonLdDocumentResolver {

    boolean canResolve(URI url);

    JsonStructure resolve(URI url);
}
```

> __NOTE__: The EDC should refrain from resolving URLs that are not mapped to the local file system. For example, HTTP URLs, due to security considerations. If end-users want this
> capability, they can implement and register a `JsonLdDocumentResolver`.


