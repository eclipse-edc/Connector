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

Command examples in this document will use the `bash` syntax and use Unix-style paths, but any other shell should be
fine as well. If you're using Windows you either need to adapt the paths or use WSL2.

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

> _The complete sample code for this chapter is in `samples/01-basic-connector/`._

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

> _The complete sample code for this chapter is in `samples/02-health-endpoint`._

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

> _The complete sample code for this chapter is in `samples/03-configuration`._

## Implement a simple file transfer

Now this is a big one:

- We'll create an additional connector, so that in the end we have two connectors, a consumer and a provider.
- The connectors will talk to each other via IDS, and the consumer will expose a REST API so that external systems (e.g.
  users) can interact with them.
- The consumer will initiate a file transfer and the provider will fulfill that request and copy a file to the desired
  location.
- Both connectors will run locally on the development machine

Also, in order to keep things organized, the code in this example has been separated into several Java modules:

- `[consumer|provider]`: this is where the "main" classes and configs reside for both the consumer and the provider
  connector
- `api`: contains the REST API extension (previously named `HealthApiExtension`)
- `transfer-file`: contains all the code necessary for the file transfer

### Create a control REST API

We will need some way to interact with the consumer, a communication protocol of sorts. That is what we call
outward-facing communication. In this example we will communicate with the consumer via simple command line tools
like `cURL`, but it is easy to imagine some other much more complicated control system to interact with the (consumer)
connector. Thus, using Jakarta, we must create an API controller the same way we created our health endpoint a few
chapters back. In fact, we can re-use and
improve [that controller](samples/04-file-transfer/api/src/main/java/org/eclipse/dataspaceconnector/extensions/api/ConsumerApiController.java)
(code omitted here for brevity).

Again, like before, the controller is instantiated and registered in an extension which we aptly
name `ApiEndpointExtension.java`.

### Create the "provider" connector

The provider is the one "owning" the data. That means, the consumer sends a request to the provider (over IDS), who then
performs the file transfer. The provider must maintain an internal list of assets that are available for transfer, the
so-called "catalog". For the sake of simplicity we use an in-memory catalog and pre-fill it with just one single class:

```java
// in FileTransferExtension.java
@Override
public void initialize(ServiceExtensionContext context){
        // ...
        registerDataEntries(context);
        // ...
        }

private void registerDataEntries(ServiceExtensionContext context){
        var metadataStore=context.getService(MetadataStore.class);

        GenericDataCatalogEntry file1=GenericDataCatalogEntry.Builder.newInstance()
        .property("type","File")
        .property("path","/path/to/assets/")
        .property("filename","test-document.txt")
        .build();

        DataEntry entry1=DataEntry.Builder.newInstance().id("test-document").policyId(USE_EU_POLICY).catalogEntry(file1).build();
        metadataStore.save(entry1);
        }
```

This adds a `GenericDataCatalogEntry` to the `MetadataStore` (which is the catalog). Or, in other words, your provider
now "hosts" one file named `test-documents.txt` located in the path `/path/to/assets/` on your development machine. It
makes it available for transfer under its `id` `"test-document"`. While it makes sense to have some sort of similarity
between file name and id, it is by no means mandatory.
> **Please adjust this path as necessary to match your local environment!**

Please also note that the registering of Policies is omitted from this document for clarity.

The `FileTransferExtension` also registers a `FileTransferFlowController`, which is basically a glorified `cp` process
with some additional checking Please note that the provider connector does *not* add the `api` module, so that means
that no REST API is offered by the provider!
In order to make use of the `FileTransferExtension`, we simply must add the correct dependency to the provider's build
file:

```kotlin
implementation(project(":samples:04-file-transfer:transfer-file"))
```

### Create the "consumer" connector

The consumer is the one "requesting" the data and providing a destination for it, i.e. a directory into which the
provider can copy the requested file.

While the consumer does not need the `transfer-file` module, it does indeed need the `api` module, which implements the
REST API:

```kotlin
implementation(project(":samples:04-file-transfer:api"))
```

It is good practice to explicitly configure the consumer's API port in `consumer/config.properties` like we learned in
previous chapters.

### Add IDS modules

Now we have a provider, that can perform a file transfer, and a consumer, that can accept commands via REST, but those
two cannot yet "talk" to each other.

The standard way of connector-to-connector communication is IDS. Thus, we must add the relevant IDS-module to both build
files:

```kotlin
// in consumer/build.gradle.kts and provider/build.gradle.kts:
implementation(project(":data-protocols:ids"))
implementation(project(":data-protocols:ids:ids-policy-mock"))
```

This adds the entire IDS protocol suite to both connectors. Nothing more needs to be done here. We will see later how we
can instruct the consumer to request a file from our provider.

### Perform a file transfer

Quick recap: we have a provider, that can handle file transfers (due to its dependency against the `transfer-file`
module), and a consumer, that can accept REST request (due to its dependency against the `api` module). They can talk to
each other because both include the IDS modules in their build config.

So all that's left is to start them both and initiate a file transfer!

Let's rebuild and run them both:

```bash
./gradlew clean build
java -Ddataspaceconnector.fs.config=samples/04-file-transfer/consumer/config.properties -jar samples/04-file-transfer/consumer/build/libs/consumer.jar
# in another terminal window:
java -Ddataspaceconnector.fs.config=samples/04-file-transfer/provider/config.properties -jar samples/04-file-transfer/provider/build/libs/provider.jar
```

Assuming you didn't change the config files, the consumer will listen on port `9191` and the provider will listen on
port `8181`. Open another terminal window (or any REST client of your choice) and execute the following REST request:

```bash
curl -X POST "http://localhost:9191/api/file/test-document?connectorAddress=http://localhost:8181/&destination=/path/on/yourmachine"
```

> **Please adjust the `destination` to match your local dev machine!**

- the last path item, `test-document`, matches the ID of the `GenericDataCatalogEntry` that we created earlier in
  `FileTransferExtension.java`, thus referencing the _data source_
- the first query parameter (`connectorAddress`) is the address of the provider connector
- the last query parameter (`destination`) indicates the desired _destination_ directory on you local machine.
- `curl` will return the ID of the transfer process on the consumer connector.

The consumer should spew out logs similar to:

```bash
INFO 2021-09-07T17:24:42.128363 Received request for file test-document against provider http://localhost:8181/
DEBUG 2021-09-07T17:24:42.592422 Request approved and acknowledged for process: 2b0a9ab8-78db-4753-8b95-77678cdd9fc8
DEBUG 2021-09-07T17:24:47.425729 Process 2b0a9ab8-78db-4753-8b95-77678cdd9fc8 is now IN_PROGRESS
DEBUG 2021-09-07T17:24:47.426115 Process 2b0a9ab8-78db-4753-8b95-77678cdd9fc8 is now COMPLETED
```

and the provider should log something like:

```bash
DEBUG 2021-09-07T17:24:42.562909 Received artifact request for: test-document
INFO 2021-09-07T17:24:42.568555 Data transfer request initiated
```

then check `/path/on/yourmachine`, which should now contain a file named `test-document.txt`.

> _The complete sample code for this chapter is in `samples/04-file-transfer`._

## Improve the file transfer

So far we have performed a file transfer on a local machine using the Eclipse Dataspace Connector. While that is already
great progress, it probably won't be much use in a real-world production application.

This chapter improves on this by moving the file transfer "to the cloud". What we mean by that is, that instead of
reading the source file from and writing the destination file to disk, we

- read the source from Azure Storage
- put the destination file into an AWS S3 Bucket.

### Setup local dev environment

Before we get into the nitty-gritty of cloud-based data transfers, we need to set up cloud resources. While we could do
that manually clicking through the Azure and AWS portals, there are simply more elegant solutions around. We use
Hashicorp Terraform for deployment and maintainance.

> You will need an active Azure Subscription and an AWS Account with root-user/admin access! Both platforms offer free tiers, so no immediate cost incurs.

Also, you will need to be logged in to your Azure CLI as well as AWS CLI by entering the following commands in a shell:

```bash
az login
aws configure
```

The deployment scripts will provision all resources in Azure and AWS (that's why you need to be logged in to the CLIs)
and store all access credentials in an Azure Vault (learn
more [here](https://azure.microsoft.com/de-de/services/key-vault/#product-overview)).

### Create a certificate for Azure

The cloud resources hosted in Azure require a certificate for authentication, so you need to create one:

```bash
# create certificate:
openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem
# create pfx file, we'll need this later
openssl pkcs12 -export -in cert.pem -inkey key.pem -out cert.pfx    
```

Of course you can tune the parameters to your liking. Due to the way how Terraform interprets environment variables, we
need to store the contents of the certificate in an environment variable named `TF_VAR_CERTIFICATE`:

```bash
TF_VAR_CERTIFICATE=$(<cert.pem)
```

### Deploy cloud resources

It's as simple as running the main terraform script:

```bash
cd samples/05-file-transfer-cloud/terraform 
terraform init
terraform apply
```

it will prompt you to enter a unique name, which will serve as prefix for many resources both in Azure and in AWS. Then,
enter "yes" and let terraform work its magic.

It shouldn't take more than a couple of minutes, and when it's done it will log the `client_id`, `tenant_id`
, `vault-name`, `storage-container-name` and `storage-account-name`.
> Take a note of these values!

### Update connector config

_Do the following for both the consumer's and the provider's `config.properties`!_

Let's modify the following config values to the connector configuration `config.properties` and insert the values that
terraform logged before:

```properties
edc.vault.clientid=<client_id>
edc.vault.tenantid=<tenant_id>
edc.vault.certificate=<path_to_pfx_file>
edc.vault.name=<vault-name>
```

### Take a look at the updated `transfer-file` and `api` module

For this chapter the file transfer extension (`CloudTransferExtension.java`) has been upgraded significantly. Most
notable are these changes:

- the extension now creates different catalog entries
- the flow controller (`BlobToS3DataFlowController.java`) is more powerful
- there are additional dependencies that take care of provisioning S3 buckets and reading blobs from Azure

Currently, we have implementations to _provision_ S3 buckets and Azure Storage accounts, but this example only contains
code to transfer data from Azure Storage to S3 (and not vice-versa). Check out the `*Reader.java` and `*Writer.java`
classes in the `transfer-file` module.

In the `api` module the `ConsumerApiController.java` has also been upgraded quite a bit. It now exposes endpoints to
start, check and deprovision transfer requests.

### Bringing it all together

While we have deployed several cloud resources in the previous chapter, the connectors themselves still run locally.
Thus, we can simply rebuild and run them:

```bash
./gradlew clean build
java -Ddataspaceconnector.fs.config=samples/05-file-transfer-cloud/consumer/config.properties -jar samples/05-file-transfer-cloud/consumer/build/libs/consumer.jar
# in another terminal window:
java -Ddataspaceconnector.fs.config=samples/05-file-transfer-cloud/provider/config.properties -jar samples/05-file-transfer-cloud/provider/build/libs/provider.jar
```

Once the connectors are up and running, we can initiate a data transfer by executing:

```bash
curl -X POST -H "Content-Type: application/json" -d @samples/05-file-transfer-cloud/datarequest.json "http://localhost:9191/api/datarequest"
```

like before that'll return a UUID. Using that UUID we can then query the status of the transfer process by executing:

```bash
curl -X GET "http://localhost:9191/api/datarequest/<UUID>/state
```

which will return one of
the [TransferProcessStates](spi/src/main/java/org/eclipse/dataspaceconnector/spi/types/domain/transfer/TransferProcessStates.java)
enum values. Once the transfer process has reached the `COMPLETED` state, we can deprovision it using

```bash
curl -X DELETE http://localhost:9191/api/datarequest/<UUID>
```

Deprovisioning is not necessary per se, but it will do some cleanup, delete the temporary AWS role and the S3 bucket, so
it's generally advisable to do it.