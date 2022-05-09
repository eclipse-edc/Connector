# Perform a contract negotiation

**Note: this sample is based on [samples/04.0-file-transfer](../../../samples/04.0-file-transfer) sample and parts of the README are taken from there as well and adapted**

Initial document and code can be found here [samples/04.0-file-transfer](../../../samples/04.0-file-transfer).

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

## Run the sample

Running this sample consists of multiple steps, that are executed one by one.

### 1. Build and start the connectors

The first step to running this sample is building and starting both the provider and the consumer connector. This is
done the same way as in the previous samples.

```bash
./gradlew samples:other:file-transfer-provisioning:consumer:build
java -Dedc.fs.config=samples/other/file-transfer-provisioning/consumer/config.properties -jar samples/other/file-transfer-provisioning/consumer/build/libs/consumer.jar
# in another terminal window:
./gradlew samples:other:file-transfer-provisioning:provider:build
java -Dedc.fs.config=samples/other/file-transfer-provisioning/provider/config.properties -jar samples/other/file-transfer-provisioning/provider/build/libs/provider.jar
````

Assuming you didn't change the ports in config files, the consumer will listen on port `9191`
and the provider will listen on port `8181`.

### 2. Initiate a contract negotiation

In order to request any data, a contract agreement has to be negotiated between provider and consumer. The provider
offers all of their assets in the form of contract offers, which are the basis for such a negotiation. In
the `transfer-file` extension, we've added a contract definition (from which contract offers can be created) for the
file, but the consumer has yet to accept this offer.

In order to trigger the negotiation, we use Data management api endpoints. We set our contract offer in the request body. The contract
offer is prepared in [contractoffer.json](contractoffer.json)
and can be used as is. In a real scenario, a potential consumer would first need to request a description of the
provider's offers in order to get the provider's contract offer.

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/file-transfer-provisioning/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"
```


### 3. Look up the contract agreement ID

After calling the endpoint for initiating a contract negotiation, we get a UUID by listing all contract and getting the UUID from the list:

```bash
curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations"
```

This will return something like:

```json
[{"contractAgreementId":"1:1ce2ca14-2c0b-42ee-9da1-1cb4bda878d7","counterPartyAddress":"http://localhost:8282/api/v1/ids/data","errorDetail":null,"id":"3d1cfb4e-fd63-4f76-8f64-19f2ca053a8d","protocol":"ids-multipart","state":"CONFIRMED","type":"CONSUMER"}]
```

### 4. Request the file

Now that we have a contract agreement, we can finally request the file. In request body we specify the name of the file
we want to have transferred and
provide the address of the provider connector, the path where we want the file copied, and the contract agreement ID as
query parameters:

```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/file-transfer-provisioning/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"
```

Again, we will get a UUID in the response. This time, this is the ID of the `TransferProcess`
created on the consumer side. Because like the contract negotiation, the data transfer is handled in a state machine and
performed asynchronously.

### 5. Look for the transfer


After calling the endpoint for initiating a transfer process, we get a UUID by listing all transfers and getting the UUID from the list:

```bash
curl -X GET -H "Content-Type: application/json" -H "X-Api-Key: password" "http://localhost:9192/api/v1/data/transferprocess"
```

This will return something like:

```json
[{"id":"48a1c453-577a-4d3e-aa83-885bcde5a546","type":"CONSUMER","state":"COMPLETED","errorDetail":null,"dataRequest":{"assetId":"test-document","contractId":"1:23901e87-e8df-491e-886c-6d35bd170efd","connectorId":"consumer"}}]
```

---

You can also check the logs of the connectors to see that the transfer has been completed:

Consumer side:

```bash
DEBUG 2022-05-03T13:18:57.3347822 Starting transfer for asset test-document
DEBUG 2022-05-03T13:18:57.3347822 Transfer process initialised 48a1c453-577a-4d3e-aa83-885bcde5a546
DEBUG 2022-05-03T13:21:13.0356666 TransferProcessManager: Sending process 48a1c453-577a-4d3e-aa83-885bcde5a546 request to http://localhost:8282/api/v1/ids/data
DEBUG 2022-05-03T13:21:13.0709799 Response received from connector. Status 200
```

### 5. See transferred file

After the file transfer is completed, we can check the destination path specified in the request for the file. Here,
we'll now find a file with the same content as the generated on the fly content offered by the provider.
