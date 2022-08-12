# Write your first extension

In the previous chapter we learned how to build and run a very simple connector. In this chapter we will learn how to
leverage the extension concept to add a simple HTTP GET endpoint to our connector.

An _extension_ typically consists of two things:

1. a class implementing the `ServiceExtension` interface.
2. a plugin file in the `src/main/resources/META-INF/services` directory. This file **must** be named exactly as the
   interface's fully qualified class-name and it **must** contain the fully-qualified name of the implementing class (
   =plugin class).

Therefore, we require an extension class, which we'll name `HealthEndpointExtension`:

```java
public class HealthEndpointExtension implements ServiceExtension {

    @Inject
    WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(new HealthApiController(context.getMonitor()));
    }
}
```

The `@Inject` annotation indicates that the extension needs a service that is registered by another extension, in 
this case an implementation of `WebService.class`.

For that, we can use Jakarta REST annotations to implement a simple REST API:

```java
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;

    public HealthApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("Received a health request");
        return "{\"response\":\"I'm alive!\"}";
    }
}
```

Once we compile and run the application with

```bash
./gradlew clean samples:02-health-endpoint:build
java -jar samples/02-health-endpoint/build/libs/connector-health.jar
```

we can issue a GET request to `http://localhost:8181/api/health` and receive the aforementioned string as a result.

It is worth noting that by default the webserver listens on port `8181`, which is defined
in [`JettyConfiguration.java`](../../extensions/http/jetty/src/main/java/org/eclipse/dataspaceconnector/extension/jetty/JettyConfiguration.java)
and can be configured using the `web.http.port` property (more on that in the next chapter). You will need to configure
this whenever you have two connectors running on the same machine.

Also, the default path is `/api/*`, which is defined
in [`JettyConfiguration.java`](../../extensions/http/jersey/src/main/java/org/eclipse/dataspaceconnector/extension/jersey/JettyConfiguration.java)
.
