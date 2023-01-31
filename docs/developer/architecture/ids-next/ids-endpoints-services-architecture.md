# IDS-Next Endpoints and Services Architecture

IDS-next requires all protocol message types to be serialized as JSON-LD. The IDS REST binding specifications further define how those serialized message types are bound to
endpoints over HTTPS. Controller endpoints will be added to the EDC that support marshalling and unmarshalling JSON-LD messages as specified
in [JSON-LD Processing Architecture document](./json-ld-processing-architecture.md)

IDS controller endpoints will be organized in two extensions to support separate deployments of control plane and catalog runtimes:

| Description          | Repository | Extension         |
|----------------------|------------|-------------------|
| Contract Negotiation | Connector  | control-plane-ids |
| Transfer Process     | Connector  | control-plane-ids |
| Catalog requests     | Connector  | catalog-ids       |

## Message (De)Marshalling

Message demarshalling will be done by deserializing IDS JSON-LD messages and expanding them using the [Titanium JSON-LD Library](https://github.com/filip26/titanium-json-ld):

```
var document = JsonDocument.of(jsonObject);
var expanded = JsonLd.expand(document).get();
```

__NOTE__: we need to verify if Jersey controllers can use `jakarta.json.JsonObject` as a type parameter directly. This will involve using the `ObjectMapper` from serialization
context configured on the `TypeManager` with Jersey. If this is not possible, the controller parameters will need to be of type `Map<String, Object>` and we will need to
transform them manually (or via a Jersey `ContainerRequestFilter`) using:

```
var converted = objectMapper.convertValue(message,JsonObject.class);
```

Message marshalling will be done by writing and compacting an in-memory `JsonObject` instance as follows:

```
var document = JsonDocument.of(jsonObject);
var compacted = JsonLd.compact(document,EMPTY_CONTEXT).get();
var compacted = mapper.convertValue(compacted,Map.class);
```

## Migrating to DCAT and ODRL Types: Catalog, Dataset, and Policy

The IDS-next specifications overhaul the infomodel that underpins IDS by basing it directly on the [DCAT](https://www.w3.org/TR/vocab-dcat-3/)
and [ODRL](https://w3c.github.io/poe/model/) specifications. One of the most significant changes is that assets (or datasets in DCAT terms) contain offers, which are ODRL policies.
Moreover, datasets are contained in a catalog. This relationship corresponds to the core design of the EDC and should resolve the complexity mandated by the current IDS infomodel
where contract offers contain assets.

### The Catalog Type

The existing `org.eclipse.edc.catalog.spi.Catalog` type will need to be migrated to the new DCAT-based model defined by IDS-Next. This section outlines three key aspects of the
required refactoring: dataset projections; distribution projections; and support for open/extensible types.

#### Dataset Projections

Instead of `ContractOffer` instances, A `Catalog` will contain `DataSet`instances that correspond to an EDC `Asset` and a collection of policies that derive from
matching `ContractDefinition`s. A replacement for the `ContractOfferResolver` will be needed that matches all `ContractDefinition`s for a `ParticipantAgent` and derives a
collection of `Dataset`s that contain `Asset` properties and ODRL Offers corresponding to the usage policies of all matching `ContractDefinition`s for the asset. This can be
represented as:

```
CD = Contract Definition
A  = Asset
DS = Dataset
O  = ODRL Offer

If the Contract Definitions are:

CD 1  --selects--> [A1, A2]
CD 2  --selects--> [A1, A3]

the resulting Catalog containing Datasets is:

DS 1 -> A1 [O:CD1,O:CD2]  
DS 2 -> A2 [O:CD1]  
DS 3 -> A3 [O:CD2]  
```

#### Distribution Projections

A DCAT Distribution is used to convey how to access a dataset: the endpoint for requesting a contract negotiation and the transport types supported by the provider. A Distribution
is therefore a combination of connector endpoint metadata and the transport type attribute of the `DataAddress` associated with an `Asset`. Since Distributions are a projection, a
single Distribution entry may be referenced by multiple Dataset instances. For example, if two assets are accessible from the same connector endpoint and have equal transport type
attributes, they will reference the same Distribution.

> The EDC does not currently have facilities for associating an asset with a connector endpoint. This will need to be defined and created.

> When a `Catalog` is generated for a request, Distributions will also need to be created for the Dataset entries. The replacement for `ContractOfferResolver` will need to track
> Distributions across Datasets as they are created. Another issue that will need to be dealt with is Assets and DataAddresses are stored independently since they are logically
> distinct. The replacement service will need access to the associated DataAddress to generate Distributions. An efficient mechanism for accessing this data will need to be
> designed.

#### Open/Extensible Types

Another consideration that will need to be made is that the DCAT `Catalog` and `Dataset` types are open: they may be arbitrarily extended using namespaced properties. Preserving
namespace information will be handled by expanding JSON-LD messages, which will concatenate referenced namespaces with properties names (which can then in turn be stored in
the `Asset` properties). The EDC will need to provide a mechanism for decorating `Catalog` instances with arbitrary properties when they are returned in response to catalog
queries. For example, an EDC provider may wish to include a catalog description property. This mechanism can be implemented by creating a registry of `CatalogDecorator`s that can
set arbitrary information as part of request processing.

In a subsequent release, it may be desirable to create a generic `CatalogDecorator` that pulls arbitrary properties from an extensible source such as a database or configuration
file.

## Type Transformation

The existing `IdsTypeTransformer` implementations will need to be re-written to accommodate the new IDS infomodel and message types. The expectation is that the number of
transformers will be greatly reduced.

`IdsTypeTransformer` for each message type will return types used by JSON-P such as `jakarta.json.JsonValue`. These types will then be set on an appropriate IDS message instance
and the latter compacted before being returned by the controller.

### Using JsonObject

When processing a request, responses may include transforming from an EDC representation to an IDS JSON-LD structure. This can be achieved using `IdsTypeTransformer`
implementations that produce `JsonObject` types. For example, assuming `org.eclipse.edc.catalog.spi.Catalog` is refactored to include a `Dataset` type as described above, a
`FromCatalogTransformer` transformer can produce a `JsonObject` as follows:

```
public class FromCatalogTransformer extends AbstractJsonLdTransformer<Catalog, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public FromCatalogTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Catalog.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable Catalog catalog, @NotNull TransformerContext context) {
        if (catalog == null) {
            return null;
        }

        var objectBuilder = jsonFactory.createObjectBuilder();

        var datasets = catalog.getDatasets().stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        objectBuilder.add(DCAT_SCHEMA + "dataset", datasets);

        // transform properties, which are generic JSON values.
        catalog.getProperties().forEach((k, v) -> objectBuilder.add(k, mapper.convertValue(v, JsonValue.class)));

        return objectBuilder.build();
    }
}
```

The `catalong-ids` extension can register the transformer as follows:

```
// registry is injected on the extension instance

builderFactory = Json.createBuilderFactory(Map.of());

var fromCatalogTransformer = new FromCatalogTransformer(builderFactory, mapper);
registry.register(fromCatalogTransformer);
```

Similarly, a `ToCatalogTransformer` can convert from a JSON=LD structure to the EDC `Catalog` type:

```
public class ToCatalogTransformer extends AbstractJsonLdTransformer<JsonObject, Catalog> {
    private static final String DCAT_CATALOG = "http://www.w3.org/ns/dcat/Catalog";
    private static final String DCAT_DATASET = "http://www.w3.org/ns/dcat/dataset";
    private static final String DCAT_DISTRIBUTION = "http://www.w3.org/ns/dcat/distribution";
    private static final String DCAT_DATA_SERVICE = "http://www.w3.org/ns/dcat/DataService";

    public ToCatalogTransformer() {
        super(JsonObject.class, Catalog.class);
    }

    @Override
    public @Nullable Catalog transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        var type = nodeType(object, context);
        if (DCAT_CATALOG.equals(type)) {
            var builder = Catalog.Builder.newInstance();

            builder.id(nodeId(object));

            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));

            return builder.build();
        }
        return null;
    }

    private void transformProperties(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (DCAT_DATASET.equals(key)) {
            transformDatasets(value, builder, context);
        } else if (DCAT_DISTRIBUTION.equals(key)) {
            transformDistributions(value, builder, context);
        } else if (DCAT_DATA_SERVICE.equals(key)) {
            transformDataServices(value, builder, context);
        } else {
            transformGenericProperty(key, value, builder, context);
        }
    }

    private void transformDatasets(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var datasets = jsonArray.stream().map(entry -> context.transform(entry, Dataset.class)).collect(toList());
            builder.datasets(datasets);
        } else if (value instanceof JsonObject) {
            var dataset = context.transform(value, Dataset.class);
            builder.dataset(dataset);
        } else {
            context.reportProblem("Invalid dataset property");
        }
    }

    private void transformDataServices(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var dataServices = jsonArray.stream().map(entry -> context.transform(entry, DataService.class)).collect(toList());
            // .... processing
        } else if (value instanceof JsonObject) {
            var dataService = context.transform(value, DataService.class);
            // .... processing
        } else {
            context.reportProblem("Invalid DataService property");
        }
    }

    private void transformDistributions(JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            var distributions = jsonArray.stream().map(entry -> context.transform(entry, Distribution.class)).collect(toList());
            // .... processing
        } else if (value instanceof JsonObject) {
            var distribution = context.transform(value, Distribution.class);
            // .... processing
        } else {
            context.reportProblem("Invalid DataService property");
        }
    }

    private void transformGenericProperty(String key, JsonValue value, Catalog.Builder builder, TransformerContext context) {
        if (value instanceof JsonArray) {
            var jsonArray = (JsonArray) value;
            if (jsonArray.isEmpty()) {
                builder.property(key, List.of());
            } else if (jsonArray.size() == 1) {
                // unwrap array
                var result = context.transform(jsonArray.get(0), Object.class);
                builder.property(key, result);
            } else {
                var result = jsonArray.stream().map(prop -> context.transform(prop, Object.class)).collect(toList());
                builder.property(key, result);
            }
        } else {
            var result = context.transform(value, Object.class);
            builder.property(key, result);
        }
    }
}
```

### Policy Transformation

When transforming policies, there should be one `IdsTypeTransformer` for the root policy type that in turn dispatches to Policy.Visitor<T> instances to
build the transformed object graph. This was not done in the original IDS transformer implementations. A sketch of the `FromPolicyTransformer` which converts an EDC `Policy`
instance to a `JsonObject` using this approach is:

```
public class FromPolicyTransformer extends AbstractJsonLdTransformer<Policy, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public FromPolicyTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(Policy.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable Policy policy, @NotNull TransformerContext context) {
        if (policy == null) {
            return null;
        }
        return policy.accept(new Visitor(context));
    }

    /**
     * Walks the policy object model, transforming it to a JsonObject.
     */
    private static class Visitor implements Policy.Visitor<JsonObject>, Rule.Visitor<JsonObject>, Constraint.Visitor<JsonObject>, Expression.Visitor<JsonObject> {
        private TransformerContext context;

        public Visitor(TransformerContext context) {
            this.context = context;
        }

        @Override
        public JsonObject visitAndConstraint(AndConstraint andConstraint) {
            for (var constraint : andConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            
            // .... create an AndConstraint representation and return it
            return jsonObject;
        }

        @Override
        public JsonObject visitOrConstraint(OrConstraint orConstraint) {
            for (var constraint : orConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            
            // .... create an OrConstraint representation and return it
            return jsonObject;
        }

        @Override
        public JsonObject visitXoneConstraint(XoneConstraint xoneConstraint) {
            for (var constraint : xoneConstraint.getConstraints()) {
                var constraintObject = constraint.accept(this);
            }
            
            // .... create an XoneConstraint representation and return it
            return jsonObject;           
        }

        @Override
        public JsonObject visitAtomicConstraint(AtomicConstraint atomicConstraint) {
            var leftObject = atomicConstraint.getLeftExpression().accept(this);
            var rightObject = atomicConstraint.getRightExpression().accept(this);
            
            // .... create an AtomicConstraint representation and return it
            return jsonObject;           
        }

        @Override
        public JsonObject visitLiteralExpression(LiteralExpression expression) {
            
            // .... create an LiteralConstraint representation and return it (may be a JsonValue)
            return jsonObject;            
        }

        @Override
        public JsonObject visitPolicy(Policy policy) {
            policy.getPermissions().forEach(permission -> permission.accept(this));
            policy.getProhibitions().forEach(prohibition -> prohibition.accept(this));
            policy.getObligations().forEach(duty -> duty.accept(this));
            
           // .... create an Policy representation and return it
            return jsonObject;
        }

        @Override
        public JsonObject visitPermission(Permission permission) {
            if (permission.getDuties() != null) {
                for (var duty : permission.getDuties()) {
                    var constraintsArray = visitRule(duty);
                }
            }
            var constraintsArray = visitRule(permission);
            
            // .... create an Permission representation and return it
            return jsonObject;
        }

        @Override
        public JsonObject visitProhibition(Prohibition prohibition) {
            var constraintsArray = visitRule(prohibition);

            // .... create an Prohibition representation and return it
            return jsonObject;
        }

        @Override
        public JsonObject visitDuty(Duty duty) {
            var constraintsArray = visitRule(duty);

            // .... create a Duty representation and return it
            return jsonObject;
        }

        private JsonArray visitRule(Rule rule) {
            // .... create an JsonArray representation and return it

            for (Constraint constraint : rule.getConstraints()) {
                var result = constraint.accept(this);
                // ... add results to the array
            }
            
            return jsonArray;
        }
    }
}
```

> Note that the policy transformers may eventually be moved to a lower-level JSON-LD extension if they can be reused by other subsystems such as the Management API extensions.

### Asset Transformation and Extensible Properties

Asset properties may be extensible complex types.

TODO Can be transformed here but DataManagement API will need to be updated

## Identity Providers

> Note: DAT tokens are no longer supported for provider responses.

> Note: The IDS specifications need a way for a catalog to declare supported trust anchors. For example, information on what identity systems are supported and where a client
> connector can obtain a security token. This can be added as part of a `CatalogDecorator`. This will require the `CatalogDecorator` to have access to the requesting participant
> agent's claims to determine which trust anchor's to include. For example, a participant agent may request data in the context of a particular dataspace that mandates a
> centralized trust anchor.

## Remote Message Dispatching

The remote message dispatching infrastructure (`RemoteMessageDispatcher`) will need to be updated to support REST-based catalog requests, transfer requests, and contract
negotiation requests. The protocol identifier will be "ids-next" until a specification version is officially assigned. This can be done in parallel to the existing IDS multipart
dispatchers.


