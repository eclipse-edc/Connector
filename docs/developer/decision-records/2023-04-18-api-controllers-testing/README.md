# Api controllers testing

## Decision

We should change the way we're testing API controllers.

## Rationale

Currently for (almost) every controller there are two test classes:
- one that test the behavior in an unit manner (calling the controller method, mocking collaborators)
- one that load a whole EDC runtime and test the behavior integrated with the rest of the services.

This approach brings in a lot of problems:
- duplicated tests
- every time a new EDC service is added somewhere, the integration tests could fail because of missing service registrations.

## Approach

The way to go would be to have a single test class for every controller that's a way between the unit and the integration approach.
In these tests we want to test the integration with the Jersey framework and the Controller behavior, to do that we will have
an abstract class that start an embedded Jetty instance with Jersey and deploys a controller.
```java
public abstract class JerseyIntegrationTestBase {
    private JettyService jetty;
    protected final int port = getFreePort();
    protected Monitor monitor = mock(Monitor.class);

    protected abstract Object controller();

    @BeforeEach
    void setup() {
        var config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("test", port, "/"));
        jetty = new JettyService(config, monitor);
        var jerseyService = new JerseyRestService(jetty, new JacksonTypeManager(), mock(JerseyConfiguration.class), monitor);
        jetty.start();
        jerseyService.registerResource("test", controller());
        jerseyService.start();
    }

    @AfterEach
    void teardown() {
        jetty.shutdown();
    }
}
```

This class could then be extended by the actual test class that will instantiate the controller with mocked collaborators,
and use `RestAssured` to call the controller methods. This way the integration with Jersey (paths, validations, serdes)
will be verified.
e.g.
```java
@ApiTest
public class AssetApiControllerIntegrationTest extends JerseyIntegrationTestBase {

    private final AssetService service = mock(AssetService.class);
    private final DataAddressResolver dataAddressResolver = mock(DataAddressResolver.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);

    @Override
    protected Object controller() {
        return new AssetApiController(monitor, service, dataAddressResolver, transformerRegistry);
    }

    @Test
    void getAllAssets() {
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(AssetResponseDto.class)))
                .thenReturn(Result.success(AssetResponseDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));

        baseRequest()
                .get("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }
}
```
