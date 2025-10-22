# E2E Testing Improvements

## Decision

We will provide new components and patterns to improve end-to-end testing capabilities for the EDC based projects.

## Rationale

Currently, we have the `RuntimePerClassExtension` which helps to set up an EDC runtime in process for testing purposes.
While it is useful when testing individual components, it has limitations when it comes to full end-to-end scenarios,
especially when multiple runtimes are involved.

We also built in different EDC projects custom extensions of `RuntimePerClassExtension` for fulfilling specific testing
needs, and accessory classes to facilitate common testing tasks like `Participant`, but this lead to code duplication
and maintenance overhead.

## Approach

When setting an EDC runtime for testing the needed information are:

- Configuration settings (properties)
- Modules to load
- Optional customizations (e.g., mocking certain services)
- Endpoints to expose (e.g., HTTP server)

This is already available in `RuntimePerClassExtension`, but when managing Endpoints, which usually
are information needed for end-to-end testing, it becomes cumbersome.

We will introduce a new testing component `ComponentRuntimeExtension` that encapsulates the above information
and provides a fluent API for configuring a runtime for testing purposes.

This component can be used to set up multiple runtimes in a single test class, each with its own configuration,
but it will also work when testing a single runtime.

```java
public class ComponentRuntimeExtension extends RuntimePerClassExtension {
    protected String name;
    // other internal information 

}
```

with an associated builder:

```java

public class ComponentRuntimeExtension extends RuntimePerClassExtension {

    public static class Builder {

        protected String name;
        protected List<String> modules = new ArrayList<>();
        protected final List<Supplier<Config>> configurationProviders = new ArrayList<>();

        protected Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder modules(String... modules) {
            this.modules.addAll(Arrays.stream(modules).toList());
            return this;
        }

        public Builder configurationProvider(Supplier<Config> configurationProvider) {
            this.configurationProviders.add(configurationProvider);
            return this;
        }

        public ComponentRuntimeExtension build() {
            Objects.requireNonNull(name, "name");

            // logic to consolidate configuration providers

        }

    }
}
```

This will provide fluent API for configuring a runtime and hiding the underlying `EmbeddedRuntime` details,
which handles the bootstrapping.

The `ComponentRuntimeExtension` since it extends `RuntimePerClassExtension` will manage the lifecycle of the runtime,
and it will inject in test methods the runtime services as needed e.g. Vault, Stores, etc.

The `ComponentRuntimeExtension` won't be directly injectable as parameter in test methods, but instead a
`ComponentRuntimeContext` will be provided

```java
public class ComponentRuntimeContext {


    public LazySupplier<URI> getEndpoint(String name) {
        // logic to retrieve the endpoint by name
    }


    public <T> T getService(Class<T> klass) {
        // logic to retrieve the service from the runtime
    }

    public Config getConfig() {
        // logic to retrieve the runtime configuration
    }

} 
```

This context will provide access to the runtime information, including the dynamic endpoints
, available services and configuration.

### Endpoints Configuration

We also need to provide a way to configure dynamic Endpoints that the runtime will expose.
This can be achieved by adding methods to the builder to register endpoints.

```java
public Builder endpoints(Endpoints endpoints) {
    // logic to register the endpoint
    return this;
}
```

with the `Endpoints` class encapsulating the details of the endpoints to be exposed.

```java
public class Endpoints {

    private final Map<String, LazySupplier<URI>> endpoints;

    private Endpoints(Map<String, LazySupplier<URI>> endpoints) {
        this.endpoints = endpoints;
    }

    @Nullable
    public LazySupplier<URI> getEndpoint(String name) {
        return endpoints.get(name);
    }

    public Map<String, LazySupplier<URI>> getEndpoints() {
        return endpoints;
    }

    public static class Builder {
        private final Map<String, Supplier<URI>> endpoints = new HashMap<>();

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpoint(String name, Supplier<URI> urlSupplier) {
            endpoints.put(name, urlSupplier);
            return this;
        }

        public Endpoints build() {
            var endpoints = this.endpoints.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), new LazySupplier<>(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new Endpoints(endpoints);
        }
    }
}
```

This will allow tests to easily configure and retrieve endpoint information for the runtimes they set up.

```java
 Endpoints ENDPOINTS = Endpoints.Builder.newInstance()
        .endpoint("default", () -> URI.create("http://localhost:" + getFreePort() + "/api"))
        .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
        .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"))
        .endpoint("protocol", () -> URI.create("http://localhost:" + getFreePort() + "/protocol"))
        .build();
```

The `Endpoints` won't be consolidated until the build is called. This will allow usage of common endpoint
across multiple runtimes.

```java
 Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
        .endpoint("default", () -> URI.create("http://localhost:" + getFreePort() + "/api"))
        .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
        .endpoint("control", () -> URI.create("http://localhost:" + getFreePort() + "/control"))
        .endpoint("protocol", () -> URI.create("http://localhost:" + getFreePort() + "/protocol"));

var providerEndpoints = ENDPOINTS.build();
var consumerEndpoints = ENDPOINTS.build();
```

Underlying the `LazySupplier` class will ensure that the endpoint URIs are only created when requested
and cached for subsequent calls.

The `ComponentRuntimeExtension` will automatically configure the runtime to expose the registered endpoints

using the convention `web.http.<name>.{port,path}`

### Dynamic Parameters injection

In end-to-end testing scenarios, it's common to have some utility classes that represent participants
or some component and exposes methods to interact with the runtime directly or via REST APIs.
Currently, we do that with custom classes like `Participant` that encapsulate the logic to interact
with the runtime or like we do with the `ManagementEndToEndTestContext`.

To facilitate this, we will introduce a new parameter injection mechanism though the `ComponentRuntimeExtension`
builder that will allow injecting custom classes into test methods by deriving information from the
`ComponentRuntimeContext`.

```java
public <T> Builder paramProvider(Class<T> klass, Function<ComponentRuntimeContext, T> paramProvider) {
    return this;
}
```

When resolving a parameter of type `T`, the `ComponentRuntimeExtension` will use the provided function
to create an instance of `T` using the `ComponentRuntimeContext`.

An example usage would be:

```java

@RegisterExtension
static final RuntimeExtension CONSUMER_RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
        // other configuration
        .paramProvider(ManagementApiClientV4.class, (ctx) -> {
            var participantId = context.getConfig().getString("edc.participant.id");
            return ManagementApiClientV4.Builder.newInstance().participantId(participantId)
                    .controlPlaneManagement(context.getEndpoint("management"))
                    .controlPlaneProtocol(context.getEndpoint("protocol"))
                    .build();
        })
        .build();
 ```

The `ManagementApiClientV4` then can be directly injected into test methods:

```java

@Test
void testManagementApi(ManagementApiClientV4 managementApiClient) {
    // use managementApiClient
}
```

### Multi-Runtime Support

When using multiple `RuntimePerClassExtension` in a single test class, JUnit will not be able to
differentiate which runtime to use for parameter injection if the same type is requested.

We will address this by introducing a new annotation `@Runtime` that can be used to specify
which runtime to use for parameter injection.

The value of the annotation will correspond to the runtime `name` provided
when building the `ComponentRuntimeExtension`.

```java

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Runtime {
    String value();
}
```

This way, when multiple runtimes are present, the test method can specify which runtime to use for a specific parameter.

```java

@Test
void testMultipleRuntimes(@Runtime("consumer") ManagementApiClientV4 consumer,
                          @Runtime("provider") ManagementApiClientV4 provider) {
    // use consumer and provider
}
```

This will be valid for custom parameter providers as well as for built-in services provided by the runtime.

