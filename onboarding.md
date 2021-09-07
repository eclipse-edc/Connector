# Onboarding and first steps

This document serves as step-by-step guide for new community members and assumes a working knowledge of the Eclipse
Dataspace Connector (EDC) nomenclature. If you do not know about the EDC nomenclature we strongly advise reading the
documentation and/or watch the introductory video.

All chapters of this guide are incremental, so e.g. example 3 uses code from example 2. All code resides in
the `samples/` directory of this repository.

Also, a working knowledge of Git, Gradle, Java and HTTP is presumed.

We'll assume that you've just checked out the EDC code base and have Java 11 installed on your development machine. The
Eclipse Dataspace Connector is built using Java 11, so we'll assume you have that installed as well. If not, please
download and install JDK 11 for your OS.

Command examples in this document will use the `bash` syntax, but any other shell should be fine as well.

## Run a basic connector

A runnable connector consists of a `Runtime` and a build file, in our case this is a `build.gradle.kts`.

The first thing we need is the `Runtime` which is the main entry point to the connector application, same as with any
other Java program. It basically uses a few bootstrap classes to get the core up and running. For an example please take
a look
at [this runtime](samples/01-basic-connector/src/main/java/org/eclipse/dataspaceconnector/runtime/ConnectorRuntime.java)
.

The second thing we need is a gradle build file (an example can be
found [here](samples/01-basic-connector/build.gradle.kts))
that contains the essential dependencies. We'll need at least the following things:

```kotlin
dependencies {
    implementation(project(":core:bootstrap"))
    implementation(project(":core:transfer"))
    implementation(project(":core:protocol:web"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))

    // junit dependencies omitted for clarity
}
```

with that we can build and run the connector from the root directory:

```bash
./gradlew clean samples:01-basic-connector:build
java -jar samples/01-basic-connector/build/libs/basic-connector.jar
```

_Note: the above snippet assumes that you did not alter the build file, i.e. the `shadow` plugin is used and the build
artifact resides at the path mentioned above. Also, we assume usage of the Gradle Wrapper, as opposed to a local Gradle
installation._

The keen-eyed reader may have noticed that the `:core:protocol:web` dependency is not strictly speaking _necessary_, but
removing it will cause the application to terminate right after it has started. The dependency causes the application to
be kept running due to an embedded Jetty being started.

If everything works out fine you should see command-line output similar to this:

```bash
INFO 2021-09-06T16:21:50.643126 Configuration file does not exist: 03-configuration.properties. Ignoring.
INFO 2021-09-06T16:21:50.66852 Secrets vault not configured. Defaulting to null vault.
INFO 2021-09-06T16:21:50.802936 Initialized Core Services extension.
INFO 2021-09-06T16:21:50.80613 Initialized Core Transfer extension
INFO 2021-09-06T16:21:50.828181 Initialized Web extension
INFO 2021-09-06T16:21:50.828686 Initialized In-Memory Transfer Process Store extension
INFO 2021-09-06T16:21:50.831001 Started Core Transfer extension
INFO 2021-09-06T16:21:50.865946 Registered Web API context at: /api/*
INFO 2021-09-06T16:21:50.885099 HTTP listening on 8181
INFO 2021-09-06T16:21:51.117412 Started Web extension
INFO 2021-09-06T16:21:51.117517 Started Initialized In-Memory Transfer Process Store extension
INFO 2021-09-06T16:21:51.117577 Dataspace Connector ready
```

This basic connector - while perfectly fine - does not offer any outward-facing API, nor does it provide any
connector-to-connector communication protocols. However, it will serve us as platform to build out more complex
scenarios.

_The contents of this chapter are in `samples/01-basic-connector/`._

## Write your first extension

In the previous chapter we learned how to build and run a very simple connector. In this chapter we will learn how to
leverage the extension concept to add a simple HTTP GET endpoint to our connector.

An _extension_ typically consists of two things:

1. a class implementing the `ServiceExtension` interface.
2. a plugin file in the `src/main/resources/META-INF/services` directory. This file **must** be named exactly as the
   interface's fully qualified class-name and it **must** contains the fully-qualified name of the implementing class (
   =plugin class).

Therefore we require an extension class, which we'll name `HealthEndpointExtension`:

```java
public class HealthEndpointExtension implements ServiceExtension {
    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);
        webService.registerController(new HealthApiController(context.getMonitor()));
    }
}
```

The `requires()` method indicates that there is a dependency onto the `"edc:webservice"` feature, which is offered by
the `WebServiceExtension.java` located in the `:core:protocol:web` module (remember how we added that to our build
file?).

The `ServiceExtensionContext` serves as registry for all resolvable services, somewhat comparable to the "module"
concept in DI frameworks like Google Guice. From it we obtain an instance of the `WebService` interface, where we can
register our API controller class.

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
        return "I'm alive!";
    }
}
```

Once we compile and run the application with

```bash
./gradlew clean samples:02-health-endpoint:build
java -jar samples/02-health-endpoint/build/libs/connector-health.jar
```

we can issue a GET request to `http://localhost:8181/api/health` and receive the aforementioned string as a result.

It is worth noting that by default the webserver listens on port `8181`, which is defined in `JettyService.java` and can
be configured using the `web.http.port` property (more on that in the next chapter). You will need to configure this
whenever you have two connectors running on the same machine.

Also, the default path is `"/api/*"`, which is defined in `JerseyRestService.java`.

_The contents of this chapter are in `samples/02-health-endpoint`._

## Use the filesystem-based configuration

So far we have not had any option to configure our system other than directly modifying code, which generally is not an
elegant way.

The Eclipse Dataspace Connector exposes configuration through its `ConfigurationExtension` interface. That is a "
special" extension in that sense that it gets loaded at a very early stage. There is also a default implementation
named `FsConfigurationExtension.java` which uses a standard Java properties file to store configuration entries.

In the previous steps we had not included that in the JAR file, so we need to add
the `:extensions:filesystem:configuration-fs` module to the dependency list:

```kotlin
dependencies {
    // ...
    implementation(project(":extensions:filesystem:configuration-fs"))
    // ...
}
```

after building and running the connector you will notice an additional log line stating that the "configuration file
does not exist":

```bash
INFO 2021-09-07T08:26:08.282159 Configuration file does not exist: dataspaceconnector-configuration.properties. Ignoring.
```

### Set up the configuration extension

By default, the `FsConfigurationExtension` expects there to be a properties file
named `dataspaceconnector-configuration.properties` located in the current directory. The name (and path) of the config
file is configurable using the `dataspaceconnector.fs.config` property, so we can customize this to our liking.

First, create a properties file in a location of your convenience,
e.g. `/etc/eclipse/dataspaceconnector/config.properties`.

```bash
mkdir -p /etc/eclipse/dataspaceconnector
touch /etc/eclipse/dataspaceconnector/config.properties
```

Second, lets reconfigure the Jetty Web Server to listen to port `9191` instead of the default `8181`. Open
the `config.properties` with a text editor of your choice and add the following line:

```properties
web.http.port=9191
```

An example file can be found [here](samples/03-configuration/config.properties). Clean, rebuild and run the connector
again, but this time passing the path to the config file:

```properties
java -Ddataspaceconnector.fs.config=/etc/eclipse/dataspaceconnector/config.properties -jar samples/03-configuration/build/libs/filsystem-config-connector.jar
```

Observing the log output we now see that the connector's REST API is exposed on port `9191` instead:

```bash
INFO 2021-09-07T08:43:04.476254 Registered Web API context at: /api/*
INFO 2021-09-07T08:43:04.503543 HTTP listening on 9191                  <--this is the relevant line
INFO 2021-09-07T08:43:04.750674 Started Web extension
```

### Add your own configuration value

Lets say we want to have a configurable log prefix in our health REST endpoint. The way to do this involves two steps:

1. add the config value to the `config.properties` file
2. access and read the config value from code

#### 1. Add the config value

Simply add a new line with an arbitrary key to your `config.properties`:

```properties
edc.samples.03.logprefix=MyLogPrefix
```

#### 2. Access the config value

The `ServiceExtensionContext` exposes a method `getSettings(String, String)` to read settings (i.e. config values)'.
Modify the code from the `HealthEndpointExtension.java` as shown below (use the one from the `samples/03-configuration`
of course):

```java
public class HealthEndpointExtension implements ServiceExtension {
    private static final String LOG_PREFIX_SETTING = "edc.samples.03.logprefix"; // this constant is new

    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var logPrefix = context.getSetting(LOG_PREFIX_SETTING, "health"); //this line is new
        var webService = context.getService(WebService.class);
        webService.registerController(new HealthApiController(context.getMonitor(), logPrefix));
    }
}
```

Next, we must modify the constructor signature of the `HealthApiController` class and store the `logPrefix` as variable:

```java

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;
    private final String logPrefix;

    public HealthApiController(Monitor monitor, String logPrefix) {
        this.monitor = monitor;
        this.logPrefix = logPrefix;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info(String.format("%s :: Received a health request", logPrefix));
        return "I'm alive!";
    }
}
```

There are a few things worth mentioning here:

- things like configuration value names should be implemented as constants, e.g. `LOG_PREFIX_SETTING` and should have a
  consistent and hierarchical naming scheme
- if a config value is not present, we should either specify a default value (i.e. `"health"`) or throw
  an `EdcException`
- configuration values should be handled in the `*Extension` class, as it's job is to set up the extension and its
  required business logic (e.g. the controller). The extension itself should not contain any business logic.
- it's better to pass the config value directly into the business logic than passing the
  entire `ServiceExtensionContext`

## Implement a simple file transfer

## Improve the file transfer

## Bringing it all together
