# Perform a contract negotiation

After successfully providing custom configuration properties to the EDC, we will perform a data transfer next: transmit
a test file from one connector to another connector. We want to keep things simple, so we will run both connectors on
the same physical machine (i.e. your development machine) and the file is transferred from one folder in the file system
to another folder. It is not difficult to imagine that instead of the local file system, the transfer happens between
more sophisticated storage locations, like a database or a cloud storage.

This is quite a big step up from the previous sample, where we ran only one connector. Those are the concrete tasks:

* Creating an additional connector, so that in the end we have two connectors, a consumer and a provider
* Providing communication between provider and consumer using IDS multipart messages
* Utilizing the data management API to interact with the connector system
* Performing a contract negotiation between provider and consumer
* Performing a file transfer
  * The consumer will initiate a file transfer
  * The provider will fulfill that request and copy a file to the desired location

Also, in order to keep things organized, the code in this example has been separated into several Java modules:

* `[consumer|provider]`: contains the configuration and build files for both the consumer and the provider connector
* `transfer-file`: contains all the code necessary for the file transfer, integrated on provider side

## Create the file transfer extension

The provider connector needs to transfer a file to the location specified by the consumer connector when the data is
requested. In order to offer any data, the provider must maintain an internal list of assets that are available for
transfer, the so-called "catalog". For the sake of simplicity we use an in-memory catalog and pre-fill it with just one
single class. The provider also needs to create a contract offer for the asset, based on which a contract agreement can
be negotiated. For this, we also use an in-memory store and add a single contract definition that is valid for the
asset.

```java
// in FileTransferExtension.java
@Override
public void initialize(ServiceExtensionContext context){
    // ...
    var policy = createPolicy();
    policyStore.save(policy);
    
    registerDataEntries(context);
    registerContractDefinition(policy.getUid());
    // ...
}

//...

private void registerDataEntries(ServiceExtensionContext context) {
    var assetPathSetting = context.getSetting(EDC_ASSET_PATH, "/tmp/provider/test-document.txt");
    var assetPath = Path.of(assetPathSetting);
    
    var dataAddress = DataAddress.Builder.newInstance()
            .property("type", "File")
            .property("path", assetPath.getParent().toString())
            .property("filename", assetPath.getFileName().toString())
            .build();
    
    var assetId = "test-document";
    var asset = Asset.Builder.newInstance().id(assetId).build();
    
    loader.accept(asset, dataAddress);
}

private void registerContractDefinition(String uid) {
    var contractDefinition = ContractDefinition.Builder.newInstance()
            .id("1")
            .accessPolicyId(uid)
            .contractPolicyId(uid)
            .selectorExpression(AssetSelectorExpression.Builder.newInstance()
                    .whenEquals(Asset.PROPERTY_ID, "test-document")
                    .build())
            .build();
    
    contractStore.save(contractDefinition);
}
```

This adds an `Asset` to the `AssetIndex` and the relative `DataAddress` to the `DataAddressResolver` through the
`AssetLoader`. Or, in other words, your provider now "hosts" one file named `test-document.txt` located in the path
configured by the setting `edc.samples.04.asset.path` on your development machine. It makes it available for transfer
under its `id` `"test-document"`. While it makes sense to have some sort of similarity between file name and id, it
is by no means mandatory.

It also adds a `ContractDefinition` with `id` `1` and a previously created `Policy` (code omitted above), that poses no
restrictions on the data usage. The `ContractDefinition` also has an `AssetSelectorExpression` defining that it is
valid for all assets with the `id` `test-document`. Thus, it is valid for the created asset.

Next to offering the file, the provider also needs to be able to transfer the file. Therefore, the `transfer-file`
module also provides the code for copying the file to a specified location (code omitted here for brevity). It contains
the [FileTransferDataSource](transfer-file/src/main/java/org/eclipse/dataspaceconnector/extensions/api/FileTransferDataSource.java)
and the [FileTransferDataSink](transfer-file/src/main/java/org/eclipse/dataspaceconnector/extensions/api/FileTransferDataSink.java)
as well as respective factories for both. The factories are registered with the `PipelineService` in the
[FileTransferExtension](transfer-file/src/main/java/org/eclipse/dataspaceconnector/extensions/api/FileTransferExtension.java),
thus making them available when a data request is processed.

## Create the connectors

After creating the required extensions, we next need to create the two connectors. For both of them we need a gradle
build file and a config file. Common dependencies we need to add to the build files on both sides are the following:

```kotlin
// in consumer/build.gradle.kts and provider/build.gradle.kts:
implementation(project(":extensions:filesystem:configuration-fs"))

implementation(project(":data-protocols:ids"))
implementation(project(":extensions:iam:iam-mock"))

implementation(project(":extensions:api:data-management"))
implementation(project(":extensions:api:auth-tokenbased"))
```

Three of these dependencies are new and have not been used in the previous samples:
1. `data-protocols:ids`: contains all IDS modules and therefore enables IDS Multipart communication with other connectors
2. `extensions:iam:iam-mock`: provides a no-op identity provider, which does not require certificates and performs no checks
3. `extensions:api:auth-tokenbased`: adds authentication for data management API endpoints

### Provider connector

As the provider connector is the one performing the file transfer after the file has been requested by the consumer, it
needs the `transfer-file` extension provided in this sample.

```kotlin
implementation(project(":samples:04.0-file-transfer:transfer-file"))
```

We also need to adjust the provider's `config.properties`. The property `edc.samples.04.asset.path` should point to an
existing file in our local environment, as this is the file that will be transferred. We also configure a separate API
context for the data management API, like we learned in previous chapter. Then we add the property
`ids.webhook.address`, which should point to our provider connector's IDS address. This is used as the callback
address during the contract negotiation. Since the IDS API is running on a different port (default is `8282`), we set
the webhook address to `http://localhost:8282` accordingly.

### Consumer connector

The consumer is the one "requesting" the data and providing a destination for it, i.e. a directory into which the
provider can copy the requested file.

We configure the consumer's API ports in `consumer/config.properties`, so that it does not use the same ports as the
provider. In the config file, we also need to configure the API key authentication, as we're going to use
endpoints from the EDC's data management API in this sample and integrated the extension for token-based API
authentication. Therefore, we add the property `edc.api.auth.key` and set it to e.g. `password`. And last, we also need
to configure the consumer's webhook address. We expose the IDS API endpoints on a different port and path than other
endpoints, so the property `ids.webhook.address` is adjusted to match the IDS API port.

## Run the sample

Running this sample consists of multiple steps, that are executed one by one.

### 1. Build and start the connectors

The first step to running this sample is building and starting both the provider and the consumer connector. This is
done the same way as in the previous samples.

```bash
./gradlew samples:04.0-file-transfer:consumer:build
java -Dedc.fs.config=samples/04.0-file-transfer/consumer/config.properties -jar samples/04.0-file-transfer/consumer/build/libs/consumer.jar
# in another terminal window:
./gradlew samples:04.0-file-transfer:provider:build
java -Dedc.fs.config=samples/04.0-file-transfer/provider/config.properties -jar samples/04.0-file-transfer/provider/build/libs/provider.jar
````

Assuming you didn't change the ports in config files, the consumer will listen on the ports `9191`, `9192`
(data management API) and `9292` (IDS API) and the provider will listen on the ports `8181`, `8182`
(data management API) and `8282` (IDS API).

### 2. Initiate a contract negotiation

In order to request any data, a contract agreement has to be negotiated between provider and consumer. The provider
offers all of their assets in the form of contract offers, which are the basis for such a negotiation. In the
`transfer-file` extension, we've added a contract definition (from which contract offers can be created) for the
file, but the consumer has yet to accept this offer.

The consumer now needs to initiate a contract negotiation sequence with the provider. That sequence looks as follows:

1. Consumer sends a contract offer to the provider (__currently, this has to be equal to the provider's offer!__)
2. Provider validates the received offer against its own offer
3. Provider either sends an agreement or a rejection, depending on the validation result
4. In case of successful validation, provider and consumer store the received agreement for later reference

Of course, this is the simplest possible negotiation sequence. Later on, both connectors can also send counter offers in
addition to just confirming or declining an offer.

In order to trigger the negotiation, we use a data management API endpoint. We set our contract offer in the request
body. The contract offer is prepared in [contractoffer.json](contractoffer.json) and can be used as is. In a real
scenario, a potential consumer would first need to request a description of the provider's offers in order to get the
provider's contract offer.

> Note, that we need to specify the `X-Api-Key` header, as we integrated token-based API authentication. The value
of the header has to match the value of the `edc.api.auth.key` property in the consumer's `config.properties`.

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"
```

In the response we'll get a UUID that we can use to get the contract agreement negotiated between provider and consumer.

Sample output:

```json
{"id":"5a6b7e22-dc7d-4135-bc98-4cc5fd1dd1ed"}
```

### 3. Look up the contract agreement ID

After calling the endpoint for initiating a contract negotiation, we get a UUID as the response. This UUID is the ID of
the ongoing contract negotiation between consumer and provider. The negotiation sequence between provider and consumer
is executed asynchronously in the background by a state machine. Once both provider and consumer either reach the
`confirmed` or the  `declined` state, the negotiation is finished. We can now use the UUID to check the current status
of the negotiation using an endpoint on the consumer side. Again, we use the `X-Api-Key` header with the same value
that's set in our consumer's `config.properties`.

```bash
curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations/{UUID}"
```

This will return information about the negotiation, which contains e.g. the current state of the negotiation and, if the
negotiation has been completed successfully, the ID of a contract agreement. We can now use this agreement to request
the file. So we copy and store the agreement ID for the next step.

Sample output:

```json
{
  ...
  "contractAgreementId":"1:45d5b9d2-97b6-4073-8e11-e0e0f44a3538",
  "state":"CONFIRMED",
  ...
}
```

If you see an output similar to the following, the negotiation has not yet been completed. In this case,
just wait for a moment and call the endpoint again.

```json
{
  ...
  "state": "REQUESTED",
  "contractAgreementId": null,
  ...
}
```

### 4. Request the file

Now that we have a contract agreement, we can finally request the file. In the request body we need to specify
which asset we want transferred, the ID of the contract agreement, the address of the provider connector and where
we want the file transferred. The request body is prepared in [filetransfer.json](filetransfer.json). Before executing
the request, insert the contract agreement ID from the previous step and adjust the destination location for the file
transfer. Then run:

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"
```

Again, we will get a UUID in the response. This time, this is the ID of the `TransferProcess` created on the consumer
side, because like the contract negotiation, the data transfer is handled in a state machine and performed asynchronously.

Sample output:

```json
{"id":"deeed974-8a43-4fd5-93ad-e1b8c26bfa44"}
```

Since transferring a file does not require any resource provisioning on either side, the transfer will be very quick and
most likely already done by the time you read the UUID.

---

You can also check the logs of the connectors to see that the transfer has been completed:

Consumer side:

```bash
DEBUG 2022-05-03T10:37:59.599642754 Starting transfer for asset asset-id
DEBUG 2022-05-03T10:37:59.6071347 Transfer process initialised f925131b-d61e-48b9-aa15-0f5e2e749064
DEBUG 2022-05-03T10:38:01.230902645 TransferProcessManager: Sending process f925131b-d61e-48b9-aa15-0f5e2e749064 request to http://localhost:8282/api/v1/ids/data
DEBUG 2022-05-03T10:38:01.260916372 Response received from connector. Status 200
DEBUG 2022-05-03T10:38:01.285641788 TransferProcessManager: Process f925131b-d61e-48b9-aa15-0f5e2e749064 is now REQUESTED
DEBUG 2022-05-03T10:38:06.246094874 Process f925131b-d61e-48b9-aa15-0f5e2e749064 is now IN_PROGRESS
DEBUG 2022-05-03T10:38:06.246755642 Process f925131b-d61e-48b9-aa15-0f5e2e749064 is now COMPLETED
```

### 5. See transferred file

After the file transfer is completed, we can check the destination path specified in the request for the file. Here,
we'll now find a file with the same content as the original file offered by the provider.
