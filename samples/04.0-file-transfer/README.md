# Perform a contract negotiation

After successfully providing custom configuration properties to the EDC, we will perform a data transfer next: transmit
a test file from one connector to another connector. We want to keep things simple, so we will run both connectors on
the same physical machine (i.e. your development machine) and the file is transferred from one folder in the file system
to another folder. It is not difficult to imagine that instead of the local file system, the transfer happens between
more sophisticated storage locations, like a database or a cloud storage.

This is quite a big step up from the previous sample, where we ran only one connector. Those are the concrete tasks:

* Creating an additional connector, so that in the end we have two connectors, a consumer and a provider
* Providing communication between provider and consumer using IDS multipart messages
* Utilize Data management API to interact with connector system.
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
        var policy=createPolicy();

        registerDataEntries(context);
        registerContractDefinition(context,policy);
        // ...
        }

//...

private void registerDataEntries(ServiceExtensionContext context){
        AssetLoader loader=context.getService(AssetLoader.class);
        String assetPathSetting=context.getSetting(EDC_ASSET_PATH,"/tmp/provider/test-document.txt");
        Path assetPath=Path.of(assetPathSetting);

        DataAddress dataAddress=DataAddress.Builder.newInstance()
        .property("type","File")
        .property("path",assetPath.getParent().toString())
        .property("filename",assetPath.getFileName().toString())
        .build();

        String assetId="test-document";
        Asset asset=Asset.Builder.newInstance().id(assetId).policyId(USE_EU_POLICY).build();

        loader.accept(asset,dataAddress);
        }

private void registerContractDefinition(ServiceExtensionContext context,Policy policy){
        ContractDefinitionStore contractStore=context.getService(ContractDefinitionStore.class);

        ContractDefinition contractDefinition=ContractDefinition.Builder.newInstance()
        .id("1")
        .accessPolicy(policy)
        .contractPolicy(policy)
        .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID,"test-document").build())
        .build();

        contractStore.save(contractDefinition);
        }
```

This adds an `Asset` to the `AssetIndex` and the relative `DataAddress` to the
`DataAddressResolver` through the `AssetLoader`. Or, in other words, your provider now "hosts"
one file named `test-document.txt` located in the path configured by the setting
`edc.samples.04.asset.path` on your development machine. It makes it available for transfer under
its `id` `"test-document"`. While it makes sense to have some sort of similarity between file name and id, it is by no
means mandatory.

It also adds a `ContractDefinition` with `id` `1` and a previously created `Policy` (code omitted above), that poses no
restrictions on the data usage. The `ContractDefinition` also has an
`AssetSelectorExpression` defining that it is valid for all assets with the `id` `test-document`. Thus, it is valid for
the created asset.

Next to offering the file, the provider also needs to be able to transfer the file. Therefore, the `transfer-file`
also provides the
[FileTransferExtension](transfer-file/src/main/java/org/eclipse/dataspaceconnector/extensions/api/FileTransferExtension.java)
, which contains the code for copying the file to a specified location (code omitted here for brevity).

## Create the connectors

After creating the required extensions, we next need to create the two connectors. For both of them we need a gradle
build file and a config file. A common dependency we need to add to the build file on both sides are the IDS extensions.
These are required to enable a communication between both connectors via IDS multipart messages.

```kotlin
// in consumer/build.gradle.kts and provider/build.gradle.kts:
implementation(project(":data-protocols:ids"))
```

This adds the IDS protocol package to both connectors. Since we are adding the IDS root module, nothing more needs to be
done here. Now, we can add the dependencies that are specific to provider or consumer.

### Provider connector

As the provider connector is the one performing the file transfer after the file has been requested by the consumer, it
needs the `transfer-file` extension provided in this sample.

```kotlin
implementation(project(":samples:04.0-file-transfer:transfer-file"))
```

We also need to adjust the provider's `config.properties`. The property `edc.samples.04.asset.path`
should point to an existing file in our local environment, as this is the file that will be transferred. We also add the
property `ids.webhook.address`, which should point to our provider connector's address. This is used as the callback
address during the contract negotiation. Since the IDS API is running on a different port (default is `8282`), we set
the webhook address to `http://localhost:8282` accordingly.

### Consumer connector

The consumer is the one "requesting" the data and providing a destination for it, i.e. a directory into which the
provider can copy the requested file.

It is good practice to explicitly configure the consumer's API port in `consumer/config.properties` like we learned in
previous chapters. In the config file, we also need to configure the API key authentication, as we're going to use an
endpoint from the EDC's control API in this sample. Therefore, we add the property `edc.api.auth.key`
and set it to e.g. `password`. And last, we also need to configure the consumer's API contexts and webhook address.
We expose the IDS API endpoints on a different port and path than other endpoints. The property `ids.webhook.address`
is adjusted to match the IDS API port.

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

Assuming you didn't change the ports in config files, the consumer will listen on port `9191`
and the provider will listen on port `8181`.

### 2. Initiate a contract negotiation

In order to request any data, a contract agreement has to be negotiated between provider and consumer. The provider
offers all of their assets in the form of contract offers, which are the basis for such a negotiation. In
the `transfer-file` extension, we've added a contract definition (from which contract offers can be created) for the
file, but the consumer has yet to accept this offer.

The consumer now needs to initiate a contract negotiation sequence with the provider. That sequence looks as follows:

1. Consumer sends a contract offer to the provider (__currently, this has to be equal to the provider's offer!__)
2. Provider validates the received offer against its own offer
3. Provider either sends an agreement or a rejection, depending on the validation result
4. In case of successful validation, provider and consumer store the received agreement for later reference

Of course, this is the simplest possible negotiation sequence. Later on, both connectors can also send counter offers in
addition to just confirming or declining an offer.

In order to trigger the negotiation, we use Data management api endpoints. We set our contract offer in the request body. The contract
offer is prepared in [contractoffer.json](contractoffer.json)
and can be used as is. In a real scenario, a potential consumer would first need to request a description of the
provider's offers in order to get the provider's contract offer.

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"
```

In the response we'll get a UUID that we can use to get the contract agreement negotiated between provider and consumer.

### 3. Look up the contract agreement ID

After calling the endpoint for initiating a contract negotiation, we get a UUID as the response. This UUID is the ID of
the ongoing contract negotiation between consumer and provider. The negotiation sequence between provider and consumer
is executed asynchronously in the background by a state machine. Once both provider and consumer either reach
the `confirmed` or the  `declined` state, the negotiation is finished. We can now use the UUID to check the
current status of the negotiation using an endpoint on the consumer side. As this endpoint is not part of this
sample's API but of the actual control API, we need to authenticate ourselves to use this endpoint. For this,
we use the `X-Api-Key` header with the same value that's set in our consumer's `config.properties`.

```bash
curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations/{UUID}"
```

This will return the current status of the negotiation and, if the negotiation has been completed successfully,
the ID of a contract agreement. We can now use this agreement to request the file. So we copy and store the
agreement ID for the next step.

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

Now that we have a contract agreement, we can finally request the file. In request body we specify the name of the file
we want to have transferred and
provide the address of the provider connector, the path where we want the file copied, and the contract agreement ID as
query parameters:

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"
```

Again, we will get a UUID in the response. This time, this is the ID of the `TransferProcess`
created on the consumer side. Because like the contract negotiation, the data transfer is handled in a state machine and
performed asynchronously.

Since transferring a file does not require any resource provisioning on either side, the transfer will be very quick and
most likely already done by the time you read the UUID.

---

You can also check the logs of the connectors to see that the transfer has been completed:

Consumer side:

```bash
DEBUG 2022-04-14T15:07:48.6106399 Starting transfer for asset test-document
DEBUG 2022-04-14T15:07:48.613652 Transfer process initialised 5ca43915-9bbf-4cb8-8764-2c0142709f3c
DEBUG 2022-04-14T15:07:56.3573442 Process 5ca43915-9bbf-4cb8-8764-2c0142709f3c is now IN_PROGRESS
DEBUG 2022-04-14T15:07:56.3583484 Process 5ca43915-9bbf-4cb8-8764-2c0142709f3c is now COMPLETED
```

### 5. See transferred file

After the file transfer is completed, we can check the destination path specified in the request for the file. Here,
we'll now find a file with the same content as the original file offered by the provider.
