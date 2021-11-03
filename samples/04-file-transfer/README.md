# Implement a simple file transfer

Now this is a big one:

- We'll create an additional connector, so that in the end we have two connectors, a consumer and a provider.
- The connectors will communicate with each other using IDS messages, and the consumer will expose a REST API so that
  external systems (e.g. users) can interact with them.
- The consumer will initiate a file transfer and the provider will fulfill that request and copy a file to the desired
  location.
- Both connectors will run locally on the development machine

Also, in order to keep things organized, the code in this example has been separated into several Java modules:

- `[consumer|provider]`: this is where the "main" classes and configs reside for both the consumer and the provider
  connector
- `api`: contains the REST API extension (previously named `HealthApiExtension`)
- `transfer-file`: contains all the code necessary for the file transfer

## Create a control REST API

We will need some way to interact with the consumer, a communication protocol of sorts. That is what we call
outward-facing communication. In this example we will communicate with the consumer via simple command line tools
like `cURL`, but it is easy to imagine some other much more complicated control system to interact with the (consumer)
connector. Thus, using Jakarta, we must create an API controller the same way we created our health endpoint a few
chapters back. In fact, we can re-use and
improve [that controller](samples/04-file-transfer/api/src/main/java/org/eclipse/dataspaceconnector/extensions/api/ConsumerApiController.java)
(code omitted here for brevity).

Again, like before, the controller is instantiated and registered in an extension which we aptly
name `ApiEndpointExtension.java`.

## Create the "provider" connector

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

    private void registerDataEntries(ServiceExtensionContext context) {
        InMemoryAssetIndex assetIndex = (InMemoryAssetIndex) context.getService(AssetIndex.class);
        InMemoryDataAddressResolver dataAddressResolver = (InMemoryDataAddressResolver) context.getService(DataAddressResolver.class);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
        .property("type", "File")
        .property("path", "/home/paul/Documents/")
        .property("filename", "test-document.txt")
        .build();

        String assetId = "test-document";
        Asset asset = Asset.Builder.newInstance().id(assetId).property(POLICY_ID, USE_EU_POLICY).build();

        assetIndex.add(asset, dataAddress);
        dataAddressResolver.add(assetId, dataAddress);
    }
```

This adds a `GenericDataCatalogEntry` to the `AssetIndex` (which is the catalog). Or, in other words, your provider
now "hosts" one file named `test-documents.txt` located in the path `/path/to/assets/` on your development machine. It
makes it available for transfer under its `id` `"test-document"`. While it makes sense to have some sort of similarity
between file name and id, it is by no means mandatory.
> **Please adjust this path as necessary to match your local environment!**

Please also note that the registering of Policies is omitted from this document for clarity.

The `FileTransferExtension` also registers a `FileTransferFlowController`, which is basically a glorified `cp` process
with some additional checking. Please note that the provider connector does *not* add the `api` module, so that means
that no REST API is offered by the provider!
In order to make use of the `FileTransferExtension`, we simply must add the correct dependency to the provider's build
file:

```kotlin
implementation(project(":samples:04-file-transfer:transfer-file"))
```

## Create the "consumer" connector

The consumer is the one "requesting" the data and providing a destination for it, i.e. a directory into which the
provider can copy the requested file.

While the consumer does not need the `transfer-file` module, it does indeed need the `api` module, which implements the
REST API:

```kotlin
implementation(project(":samples:04-file-transfer:api"))
```

It is good practice to explicitly configure the consumer's API port in `consumer/config.properties` like we learned in
previous chapters.

## Add IDS modules

Now we have a provider, that can perform a file transfer, and a consumer, that can accept commands via REST, but those
two cannot yet "talk" to each other.

The standard way of connector-to-connector communication is IDS. Thus, we must add the relevant IDS-module to both build
files:

```kotlin
// in consumer/build.gradle.kts and provider/build.gradle.kts:
implementation(project(":data-protocols:ids"))
implementation(project(":data-protocols:ids:ids-policy-mock"))
```

This adds the IDS protocol package to both connectors. Since we're adding the IDS root module, nothing more needs to be
done here. The `ids-policy-mock` module is added explicitly so that the extension inside it gets picket up. We will see
later how we can instruct the consumer to request a file from our provider.

## Perform a file transfer

Quick recap: we have a provider, that can handle file transfers (due to its dependency against the `transfer-file`
module), and a consumer, that can accept REST request (due to its dependency against the `api` module). They can talk to
each other because both include the IDS modules in their build config.

So all that's left is to start them both and initiate a file transfer!

Let's rebuild and run them both:

```bash
./gradlew clean build
java -Dedc.fs.config=samples/04-file-transfer/consumer/config.properties -jar samples/04-file-transfer/consumer/build/libs/consumer.jar
# in another terminal window:
java -Dedc.fs.config=samples/04-file-transfer/provider/config.properties -jar samples/04-file-transfer/provider/build/libs/provider.jar
````

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