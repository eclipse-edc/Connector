# Use the filesystem-based configuration

So far we have not had any way to configure our system other than directly modifying code, which generally is not an
elegant way.

The Eclipse Dataspace Connector exposes configuration through its `ConfigurationExtension` interface. That is a "
special" extension in that sense that it gets loaded at a very early stage. There is also a default implementation
named [`FsConfigurationExtension.java`](../../extensions/filesystem/configuration-fs/src/main/java/org/eclipse/dataspaceconnector/configuration/fs/FsConfigurationExtension.java)
which uses a standard Java properties file to store configuration entries.

In the previous steps we had not included that in the JAR file, so we need to add
the `:extensions:filesystem:configuration-fs` module to the dependency list:

```kotlin
dependencies {
    // ...
    implementation(project(":extensions:filesystem:configuration-fs"))
    // ...
}
```

We compile and run the application with:

```bash
./gradlew clean samples:03-configuration:build
java -jar samples/03-configuration/build/libs/filsystem-config-connector.jar
```

you will notice an additional log line stating that the "configuration file does not exist":

```bash
INFO 2021-09-07T08:26:08.282159 Configuration file does not exist: dataspaceconnector-configuration.properties. Ignoring.
```

## Set up the configuration extension

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

An example file can be found [here](config.properties). Clean, rebuild and run the connector again, but this time
passing the path to the config file:

```bash
java -Dedc.fs.config=/etc/eclipse/dataspaceconnector/config.properties -jar samples/03-configuration/build/libs/filsystem-config-connector.jar
```

Observing the log output we now see that the connector's REST API is exposed on port `9191` instead:

```bash
INFO 2021-09-07T08:43:04.476254 Registered Web API context at: /api/*
INFO 2021-09-07T08:43:04.503543 HTTP listening on 9191                  <--this is the relevant line
INFO 2021-09-07T08:43:04.750674 Started Web extension
```

## Add your own configuration value

Lets say we want to have a configurable log prefix in our health REST endpoint. The way to do this involves two steps:

1. add the config value to the `config.properties` file
2. access and read the config value from code

### 1. Add the config value

Simply add a new line with an arbitrary key to your `config.properties`:

```properties
edc.samples.03.logprefix=MyLogPrefix
```

### 2. Access the config value

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

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
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
        return "{\"response\":\"I'm alive!\"}";
    }
}
```

There are a few things worth mentioning here:

- things like configuration value names should be implemented as constants, e.g. `LOG_PREFIX_SETTING` and should have a
  consistent and hierarchical naming scheme
- if a config value is not present, we should either specify a default value (i.e. `"health"`) or throw
  an `EdcException`
- configuration values should be handled in the `*Extension` class, as it's job is to set up the extension and its
  required business logic (e.g. the controller). The extension itself should not contain any business logic
- it's better to pass the config value directly into the business logic than passing the
  entire `ServiceExtensionContext`, using configuration objects when there are more than one

## Management API

Part of most connectors will be the management api defined in the
[`:extensions:api:data-management`](../../extensions/api/data-management) module. Therefor, we need to add the module to
the dependency list in our `build.gradle.kts`:

```kotlin
dependencies {
    // ...
    implementation(project(":extensions:api:data-management"))
    // ...
}
```

As described in the [README.md](../../extensions/api/data-management/api-configuration/README.md) of
the [api-configuration module](../../extensions/api/data-management/api-configuration), the management api should be
exposed on a separate jetty context. Therefor, it is necessary to provide the following configuration to the connector:

> Note: The ports could be chosen arbitrarily. In this example, they are aligned to the already existing `web.http.port` setting described above.

```properties
web.http.port=9191
web.http.path=/api
web.http.data.port=9192
web.http.data.path=/api/v1/data
```

_**Caution**: If you do not provide this configuration, it leads to the problem that the authentication mechanism is
also applied to EVERY request in the _default_ context of Jetty, which includes the IDS communication between two
connectors._
