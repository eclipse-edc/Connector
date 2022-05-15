## Modules
- ids-api-[x]-endpoint-v[x] modules marshall requests and responses between the EDC and a transport. They are tied to a specific infomodel version.
- ids-transform-v[x] modules contain transformers. They are tied to a specific infomodel version. EDC model to IDS infomodel and vice versa.
- Two different endpoint modules may use the same transformers if they share a common infomodel.

## Controller
- `MultipartController` reads messages from a transport and dispatches to a Message Handler based on `RequestMessage` type.
- Performs basic validation and may return an error response

##Message Handler
- `MultipartRequestHandler` processes a `RequestMessage` type (subclass).
- Performs validation and may return an error response

###Sub Handler
- `DescriptionRequestHandler` processes a `DescriptionRequestMessage` by delegating to a _**Sub Handler**_. 
- This handler directly maintains a map of Sub Handlers keyed by the scheme-specific part of the `requestedElement` field.
- The Connector Description Sub Handler is dispatched to as the default handler. This Sub Handler will use the `ConnectorDescriptorService`.

##Transformers
- A `TransformerRegistery` dispatches to `Transformers` to map between IDS and EDC types. Handlers/Sub Handlers should use these to marshall requests and responses to and from the 
  EDC core.
- Type mapping should preferably be done in the handlers, not services (we need to look at this further, e.g. `ConnectorDescriptionService`). 

