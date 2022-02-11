# Integration testing

## Decision

Extend the existing `EdcExtension` JUnit facility (that can currently run a single EDC runtime and supports stubbing and extension of runtime services) as follows:

- Load more than one EDC runtime.
- Load EDC runtimes in separate Class Loaders.
- Run each EDC runtime Class Loader with its effective runtime class path, based on its module's Gradle configuration.

## Rationale

The need to provide an integration test harness that supports multiple runtimes emerges from multiple needs:

- Stabilizing [samples](../../samples) that run multiple connectors, which have been breaking frequently.
- Testing system behavior when multiple connectors interact, e.g. the contract negotiation process.
- Testing system behavior upon component failure.
- Providing a test facility for factoring out application components to separate runtimes (e.g. [DPF](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/463)).

Key drivers for the choice are:

- Fast and efficient run in CI.
- Fast "inner loop" (i.e. ability to quickly rerun tests after changing code) and debuggability for developers.
- Use of existing frameworks, stability and portability.

We have performed technical spikes testing multiple approaches (detailed further below), including various combinations of:

- JUnit
- [Docker compose](https://docs.docker.com/compose/)
- Starting custom Class Loaders for separate threads for the Provider and Connector with separate class paths. Using distinct Class Loaders (and threads) for each service effectively provides full runtime isolation within a single JVM
- [Testcontainers](https://www.testcontainers.org/) with custom containers was also evaluated, but support for bidirectional communication to host is complex, and we didn't manage to get it running.

Spinning additional Class Loaders for runtimes with JUnit provides very fast inner loop. Using the Gradle Classpath is [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) and ensures the runtime under test exactly matches the standalone one.

In contrast, approaches based on Docker have a slow inner loop and require rebuild between runs.

The approach used is not limited to the Dataspace Connector, it can be used to run any Java module if required in the future.

## Spikes

We have performed technical spikes on [sample 04.0-file-transfer](../../../samples/04.0-file-transfer/README.md), that runs two EDC connectors, a Consumer and a Provider. [Spike code is in a forked repository](https://github.com/agera-edc/DataSpaceConnector/pull/3/files). The test requires three components:

- Consumer EDC connector
- Provider EDC connector
- HTTP client code to interact with the Consumer provider API

We have written code for the following three options:

- **Docker-compose** which works but provides an inconvenient inner loop.
- **Class Loader with Gradle Classpath**, which uses Gradle to determine the effective class path for each runtime, and is very efficient. It is therefore the base for the recommendation above.
- **Class Loader with Shadow JAR**, a variant of the above which uses the Shadow JAR, and provides a less efficient inner loop.

### Docker-compose

In this setup, we run both connectors in Docker containers. Once they are up, we run HTTP client code in a JUnit test running on the host.

```yaml
# docker-compose.yaml
services:
  sample04-connector-provider:
    build:
      context: .
      target: sample04-connector-provider
    ports:
      - "8181:8181"
    volumes:
      - /tmp/provider:/tmp/provider
      - /tmp/consumer:/tmp/consumer

  sample04-connector-consumer:
    build:
      context: .
      target: sample04-connector-consumer
    ports:
      - "9191:9191"
    volumes:
      - /tmp/provider:/tmp/provider
      - /tmp/consumer:/tmp/consumer

```

```shell
$ (cd samples/04.0-file-transfer && docker-compose up -d)

[...]
Successfully built 26b639e8f852
Successfully tagged 040-file-transfer_sample04-connector-consumer:latest

[+] Running 3/3
 ⠿ Network 040-file-transfer_default      Created                                                                                                                                0.0s
 ⠿ Container sample04-connector-provider  Started                                                                                                                                0.7s
 ⠿ Container sample04-connector-consumer  Started                                                                                                                                0.6s
```

```shell
RUN_INTEGRATION_TEST=true EDC_PROVIDER_CONNECTOR_HOST=http://sample04-connector-provider:8181 time ./gradlew cleanTest :samples:04.0-file-transfer:integration-tests:test --tests org.eclipse.dataspaceconnector.samples.FileTransferSystemTest
```

This setup is stable and straightforward and also resembles a real deployment scenario in which provider and consumer connectors are running as a separate independent java process, but the inner loop is not very efficient for local development as to reload every new code change connector jars needs to rebuild and restart docker process to use latest jars. Debugging a remote process is possible but adds complexity.

For faster local development/debugging one efficient approach could be to run connectors code in debug mode within IDE and necessary IDE run configurations can be provided as xml config files. These run configurations also can be committed in the repository to reduce local development environment setup effort.

### Class Loader with Gradle Classpath

The integration tests module only contains the classpath of the Provider module:

```kotlin
    implementation(project(":samples:04.0-file-transfer:provider"))
```

The JUnit integration test runs the Provider using the preexisting `EdcExtension`, and the Consumer using a newly developed extension:

```java
// EDC Consumer runtime
@RegisterExtension
static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
        ":samples:04.0-file-transfer:consumer", // Gradle module of the runtime to be started
        "consumer", // prefix for console log output
        Map.of( // settings
        "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
        "edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH,
        "ids.webhook.address", CONSUMER_CONNECTOR_HOST));

// EDC Provider runtime
@RegisterExtension
static EdcRuntimeExtension provider = new EdcRuntimeExtension(
        ":samples:04.0-file-transfer:provider",
        "provider",
        Map.of(...));
```

The extension determines the class path by running a custom Gradle task:

```java
// EdcRuntimeExtension.java
Runtime.getRuntime().exec("/gradlew -q " + moduleName + ":printClasspath");

// ... process classpath (see below) ...

// run a thread with a custom class loader
var classLoader = URLClassLoader.newInstance(classPathEntries,
                                             ClassLoader.getSystemClassLoader());

var mainClassName = BaseRuntime.class.getCanonicalName();
var mainClass = classLoader.loadClass(mainClassName);
var mainMethod = mainClass.getMethod("main", String[].class);

runtimeThread = new Thread(() ->
                           {
                             Thread.currentThread().setContextClassLoader(classLoader);
                             mainMethod.invoke(null, new Object[]{new String[0]});
                           })
```

```kotlin
     // build.gradle.kts (under allprojects)
     tasks.register("printClasspath") {
     doLast {
        println("${sourceSets["main"].runtimeClasspath.asPath}");
        }
    }
```

The Gradle classpath is composed of JARs within the Connector project (e.g. `/path/to/EclipseDataSpaceConnector/extensions/api/control/build/libs/control-0.0.1-SNAPSHOT.jar`) and JARs from the Gradle cache (e.g. `/home/user/.gradle/caches/modules-2/files-2.1/org.eclipse.jetty/jetty-util/11.0.6/292fa5d7b2cef3483da8a7fa9dd608bfc9896564/jetty-util-11.0.6.jar`). The Extension code replaces JAR entries of Connector modules with their compiled classes and resource directories (`build/classes/java/main/` and `build/resources/main/`), and loads the connector within a separate Class Loader.

Setting up the Test task to pass standard output:

```kotlin
// samples/04.0-file-transfer/integration-tests/build.gradle.kts
tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
```

The test runs in seconds.

```shell
$ time ./gradlew cleanTest :samples:04.0-file-transfer:integration-tests:test --tests org.eclipse.dataspaceconnector.samples.ClassLoaderWithGradleClasspathTest

> Configure project :
> No version was specified, setting default 0.0.1-SNAPSHOT
> If you want to change this, supply the -Pversion=X.Y.Z parameter


> Task :samples:04.0-file-transfer:integration-tests:test

ClassLoaderWithGradleClasspathTest STANDARD_OUT
    INFO 2022-02-03T17:19:42.201218 Configuration file does not exist: dataspaceconnector-configuration.properties. Ignoring.
    INFO 2022-02-03T17:19:42.21943 Initialized FS Configuration
    INFO 2022-02-03T17:19:42.228993 Secrets vault not configured. Defaulting to null vault.
    INFO 2022-02-03T17:19:42.229459 Initialized Null Vault
    INFO 2022-02-03T17:19:42.469222 Initialized Core Services
    INFO 2022-02-03T17:19:42.470778 Initialized In-Memory Asset Index
    INFO 2022-02-03T17:19:42.472156 Initialized In-Memory Contract Definition Store
    INFO 2022-02-03T17:19:42.480915 Initialized Core Contract Service
    INFO 2022-02-03T17:19:42.481961 Initialized In-Memory Transfer Process Store
    INFO 2022-02-03T17:19:42.483762 Initialized org.eclipse.dataspaceconnector.transfer.core.CommandExtension
    INFO 2022-02-03T17:19:42.489996 Initialized Core Transfer
    INFO 2022-02-03T17:19:42.490808 Initialized Mock IAM
[...]
    INFO 2022-02-03T17:19:42.673829 HTTP listening on 9191
    INFO 2022-02-03T17:19:42.76275 Started Jetty Service
    INFO 2022-02-03T17:19:43.228771 Registered Web API context at: /api/*
    INFO 2022-02-03T17:19:43.229002 Started Jersey Web Service
    INFO 2022-02-03T17:19:43.229442 Started IDS Multipart API
    INFO 2022-02-03T17:19:43.22957 Started org.eclipse.dataspaceconnector.extensions.api.FileTransferExtension
    INFO 2022-02-03T17:19:43.229659 Started IDS Multipart Dispatcher API
    INFO 2022-02-03T17:19:43.229751 Started IDS Transform Extension
    INFO 2022-02-03T17:19:43.229837 Started EDC Control API
    INFO 2022-02-03T17:19:43.229915 Started API Endpoint
    INFO 2022-02-03T17:19:43.230525 edc-ce5e1eb0-62ea-43d0-858a-e48118e97d85 ready

ClassLoaderWithGradleClasspathTest > transferFile_success() STANDARD_OUT
    INFO 2022-02-03T17:19:43.666544 Configuration file does not exist: dataspaceconnector-configuration.properties. Ignoring.
    INFO 2022-02-03T17:19:43.666879 Initialized FS Configuration
    INFO 2022-02-03T17:19:43.667783 Secrets vault not configured. Defaulting to null vault.
    INFO 2022-02-03T17:19:43.667928 Initialized Null Vault
    INFO 2022-02-03T17:19:43.677249 Initialized Core Services
    INFO 2022-02-03T17:19:43.677423 Initialized In-Memory Asset Index
    INFO 2022-02-03T17:19:43.677543 Initialized In-Memory Contract Definition Store
    INFO 2022-02-03T17:19:43.677745 Initialized Core Contract Service
    INFO 2022-02-03T17:19:43.677868 Initialized In-Memory Transfer Process Store
    INFO 2022-02-03T17:19:43.677988 Initialized org.eclipse.dataspaceconnector.transfer.core.CommandExtension
    INFO 2022-02-03T17:19:43.678098 Initialized Core Transfer
    INFO 2022-02-03T17:19:43.678255 Initialized Mock IAM
[...]
    INFO 2022-02-03T17:19:43.685601 Started In-Memory Contract Negotiation Store
    INFO 2022-02-03T17:19:43.686003 HTTP listening on 8181
    INFO 2022-02-03T17:19:43.689505 Started Jetty Service

ClassLoaderWithGradleClasspathTest > transferFile_success() STANDARD_OUT
    INFO 2022-02-03T17:19:43.736455 Registered Web API context at: /api/*
    INFO 2022-02-03T17:19:43.736607 Started Jersey Web Service
    INFO 2022-02-03T17:19:43.736657 Started IDS Multipart API
    INFO 2022-02-03T17:19:43.736695 Started org.eclipse.dataspaceconnector.extensions.api.FileTransferExtension
    INFO 2022-02-03T17:19:43.736754 Started IDS Multipart Dispatcher API
    INFO 2022-02-03T17:19:43.73684 Started IDS Transform Extension
    DEBUG 2022-02-03T17:19:48.434665 [Consumer] ContractNegotiation initiated. aa9a36be-af41-4da4-b198-711389690558 is now in state INITIAL.
    DEBUG 2022-02-03T17:19:52.85985 [Provider] ContractNegotiation initiated. f8b7bb63-cdb3-4c17-8969-aaa0b868d5b3 is now in state REQUESTED.
    DEBUG 2022-02-03T17:19:52.864953 [Provider] Contract offer received. Will be approved.
    DEBUG 2022-02-03T17:19:52.865395 [Provider] ContractNegotiation f8b7bb63-cdb3-4c17-8969-aaa0b868d5b3 is now in state CONFIRMING.
    DEBUG 2022-02-03T17:19:52.876959 Response received from connector. Status 200
    DEBUG 2022-02-03T17:19:52.889059 [Consumer] ContractNegotiation aa9a36be-af41-4da4-b198-711389690558 is now in state REQUESTED.
    DEBUG 2022-02-03T17:19:53.743579 [Consumer] Contract agreement received. Validation successful.
    DEBUG 2022-02-03T17:19:53.744268 [Consumer] ContractNegotiation aa9a36be-af41-4da4-b198-711389690558 is now in state CONFIRMED.
    DEBUG 2022-02-03T17:19:53.748143 Response received from connector. Status 200
    DEBUG 2022-02-03T17:19:53.754331 [Provider] ContractNegotiation f8b7bb63-cdb3-4c17-8969-aaa0b868d5b3 is now in state CONFIRMED.
    INFO 2022-02-03T17:19:53.831271 Received request for file test-document against provider http://localhost:8181/api/ids/multipart
    DEBUG 2022-02-03T17:19:57.647989 Response received from connector. Status 200
    INFO 2022-02-03T17:19:57.650468 Object received: org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message.MultipartRequestInProcessResponse@4eeb3196
    INFO 2022-02-03T17:19:58.687893 Copying data from File to File
    INFO 2022-02-03T17:19:58.690052 Successfully copied file to /tmp/consumer/test-document.txt
    DEBUG 2022-02-03T17:20:02.640684 Process 60e34667-c51b-4a38-bb08-ddd4d705c234 is now COMPLETED
[...]
    INFO 2022-02-03T17:20:02.68389 Connector shutdown complete

BUILD SUCCESSFUL in 33s
240 actionable tasks: 2 executed, 238 up-to-date
1.89user 0.11system 0:34.37elapsed 5%CPU
```

Running in an IDE (IntelliJ IDEA), the debugger can be used in both Provider and Consumer code. When a class source is modified and the test is rerun, the change is immediately reflected.

We could have run the equivalent setup inverting the Provider and Consumer in the two extensions we used. In this infrastructure, only one of the connectors can use the `EdcExtension` facilities to add or mock services. This exploratory spike code was built as a quick-and-dirty PoC. When adapting this spike code for merging, we should improve this setup, by extending the `EdcExtension` to provide both classpath isolation and test runtime configurability.

Note that this setup requires the modules to have previously been built (`./gradlew build`).

### Class Loader with Shadow JAR

The setup is very similar to the above, but the Shadow JAR of the connector is run:

```java
    // EDC Consumer runtime
    @RegisterExtension
    @Order(1)
    static JarRuntimeExtension otherConnector = new JarRuntimeExtension(
            "../consumer/build/libs/consumer.jar",
            Map.of(
                    "web.http.port", "9191",
                    "edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH,
                    "ids.webhook.address", "http://localhost:9191"));


    // EDC Provider runtime
    @RegisterExtension
    @Order(2)
    static EdcExtension edc = new EdcExtension();
```

```java
// JarRuntimeExtension
var classLoader = URLClassLoader.newInstance(new URL[]{jarFile.toURI().toURL()},
                        ClassLoader.getSystemClassLoader());
Thread.currentThread().setContextClassLoader(classLoader);

var mainClass = classLoader.loadClass(mainClassName);
var mainMethod = mainClass.getMethod("main", String[].class);
mainMethod.invoke(null, new Object[]{new String[0]});
```

The setup is slightly simpler than the previous one, but the inner loop is less efficient: the Gradle `shadowJar` task must be rerun between test runs.
