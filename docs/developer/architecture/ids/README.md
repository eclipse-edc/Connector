# IDS Standard

See implementation details [here](../../../../data-protocols/ids/).

## Specification

### Compliance Issues

The EDC will **not** be IDS compliant in every way. This section contains a list of issues, where the non-compliance is
a conscious decision.

##### 1. No Self-Description Response at API Root

At the root path of the API IDS requires the connector to return a self-description. This is a requirement the connector
will never fulfil. The self-description is only returned on the corresponding REST or Multipart requests.

#### 2. Only one Information Model version supported at a time

The EDC connector will not be able to support more than one IDS Information model per running instance.

## Key Components

![Dispatch Architecture](dispatch.architecture.jpg)

### Modules
- ids-api-[x]-endpoint-v[x] modules marshall requests and responses between the EDC and a transport. They are tied to a specific infomodel version.
- ids-transform-v[x] modules contain transformers. They are tied to a specific infomodel version. EDC model to IDS infomodel and vice versa.
- Two different endpoint modules may use the same transformers if they share a common infomodel.

### Controller
- `MultipartController` reads messages from a transport and dispatches to a Message Handler based on `RequestMessage` type.
- Performs basic validation and may return an error response

### Message Handler
- `MultipartRequestHandler` processes a `RequestMessage` type (subclass).
- Performs validation and may return an error response

#### Sub Handler
- `DescriptionRequestHandler` processes a `DescriptionRequestMessage` by delegating to a _**Sub Handler**_.
- This handler directly maintains a map of Sub Handlers keyed by the scheme-specific part of the `requestedElement` field.
- The Connector Description Sub Handler is dispatched to as the default handler. This Sub Handler will use the `ConnectorDescriptorService`.

### Transformers
- A `TransformerRegistery` dispatches to `Transformers` to map between IDS and EDC types. Handlers/Sub Handlers should use these to marshal 
requests and responses to and from the EDC core.
- Type mapping should preferably be done in the handlers, not services (we need to look at this further, e.g. `ConnectorDescriptionService`). 

## Refactorings for Architectural Alignment (2021-10-22)

1. Flatten `/request/handler` to `/handler`. Retain `/description` subpackage (seemingly minor but this created a lot of confusion).
2. Rename `/http` to `/message` (minor, but easy to do)
3. Remove ConfigurationProvider and its implementations; replace with fail-fast POJOs that are specific to each requiring service. Remove this interface from `ids.spi`.
4. Remove all other classes in `ids.spi.version`. There should be one version type (not separate inbound/outbound) and that should be explicitly set as a code constant, not obtained
   from an artifact assumed to be on the classpath or in a manifest.
5. Remove `RejectionMultipartRequestHandler` and `RejectionMessageFactory`
6. Remove `DescriptionRequestMessageHandlerRegistry` and handle dispatching to Sub Handlers directly in `DescriptionRequestHandler`.
7. Remove `ResourceCatalogFactory` and place any logic in the `ConnectorDescriptionService`. If this logic needs to be factored out in the future, create another class and pass it
   to the ctor.
8. Ditto for `DescriptionResponseMessageFactory`.
9. Ditto for `BaseConnectorFactory` and remove one line methods.

### Discussion Points

1. IdsId type is complicated and seems to do multiple things.

   a. IdsId parse(String urn) - Can that be moved to the Description sub handler for dispatching? Also, just take the URI, get the scheme-specific part and tokenize it. Don't
   convert to string first.

   b. The enum type does not appear to offer any value if the `DescriptionRequestHandler` handles dispatching to sub handlers.  
   c. The id methods should be removed and handled by a `IdsTypeTransformer`.


2. Method `handle(DescriptionRequestMessage descriptionRequestMessage, String payload)` - Why is payload a string, should it be a type (e.g. a materialized type or Void)? - Leave as string fro now


### Other

1. gradle.properties should not be in ids-api-multipart-endpoint-v1 - there is one properties file with the info-model version in it in the root project (easy to do)
2. Create an interface for ConnectorDescriptionService
3. ContractOffer should use Unix Epoch longs instead of ZonedDateTime
4. Security Profile should default to NONE
5. ConnectorDescriptionRequestHandler should not return a null MultipartResponse:

     ```
      if (!isRequestingCurrentConnectorsDescription(descriptionRequestMessage)) {
            return null;
      }
   ```
