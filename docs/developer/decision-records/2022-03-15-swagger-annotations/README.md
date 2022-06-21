# Swagger annotations

## Decision
To avoid overloading `*Controller` api classes with too many Swagger annotations, 
every one of these classes them must implement an interface that reports all the methods exposed (public).
This decision affects all the extensions that expose a REST api.

The interfaces will be annotated all the swagger annotations needed to improve documentation, e.g.
```
@OpenAPIDefinition
public interface AssetApi
```

While the controller implementation will only be annotated with the Jakarta annotations, e.g.
```
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/assets")
public class AssetApiController implements AssetApi
```

In this context naming is also important to keep things tidy.
The interface should be called `<context>Api`, and the controller class that implements it `<context>ApiController`.