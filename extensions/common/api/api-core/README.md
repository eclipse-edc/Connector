# API Core

contains central elements of any API functionality, such as Exception mappers, etc.

## Using a `DtoTransformer`

Let's assume that there is a controller class `ExampleApiController` with at least a `GET` endpoint 
`GET /api/v1/example/objects` which returns a list of `ExampleObject`s.
And there would also be an `ExampleObjectDto`. All these things are implemented in an
`:extensions:api:example` extension.

1. Create a `ExampleObjectTransformer`:

```java
public class ExampleObjectTransformer implements DtoTransformer<ExampleObject, ExampleObjectDto>{
    // implementation
} 
```

2. Register the transformer in your `ExampleApiExtension`:
```java
//ExampleApiExtension.java

@Provides(DtoTransformerRegistry.class)
public class ExampleApiExtension implements ServiceExtension{
    @Inject(required=false)
    private DtoTransformerRegistry registry;
    
    @Inject
    private WebService webService;
    
    @Override
    public void initialize(ServiceExtensionContext context){
        
        if(registry == null){
            registry= new DtoTransformerRegistryImpl();
            context.registerService(DtoTransformerRegistry.class, registry);
        }
        
        //register the transformer
        var transformer = new ExampleObjectTransformer(/*params*/);
        registry.register(transformer);
        
        //register controller
        var ctrl= new ExampleApiController(registry);
        webService.registerResource(ctrl); 
    }
}
```

3. Use the transformer in the controller:

```java

import java.util.stream.Collectors;

public class ExampleApiController {
    // ...

    @GET
    @Path(/*the path*/)
    public List<ExampleObjectDto> getAll() {

        Stream<ExampleObject> objects = fetchFromSomewhere();

        // not shown: error/failure handling
        return objects.map(o -> registry.transform(o, ExampleObjectDto.class))
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

    }

}
```