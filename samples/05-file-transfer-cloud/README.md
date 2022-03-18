# Improve the file transfer

So far we have performed a file transfer on a local machine using the Eclipse Dataspace Connector. While that is already
great progress, it probably won't be much use in a real-world production application.

This chapter improves on this by moving the file transfer "to the cloud". What we mean by that is, that instead of
reading the source file from and writing the destination file to disk, we

- read the source from Azure Storage
- put the destination file into an AWS S3 Bucket.

## Setup local dev environment

Before we get into the nitty-gritty of cloud-based data transfers, we need to set up cloud resources. While we could do
that manually clicking through the Azure and AWS portals, there are simply more elegant solutions around. We use
Hashicorp Terraform for deployment and maintenance.

> You will need an active Azure Subscription and an AWS Account with root-user/admin access! Both platforms offer free tiers, so no immediate cost incurs.

Also, you will need to be logged in to your Azure CLI as well as AWS CLI by entering the following commands in a shell:

```bash
az login
aws configure
```

The deployment scripts will provision all resources in Azure and AWS (that's why you need to be logged in to the CLIs)
and store all access credentials in an Azure Vault (learn
more [here](https://azure.microsoft.com/de-de/services/key-vault/#product-overview)).

## Deploy cloud resources

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

Download the certificate used to authenticate the generated service principal against Azure Active Directory:

```bash
terraform output -raw certificate | base64 --decode > cert.pfx
```

## Update connector config

_Do the following for both the consumer's and the provider's `config.properties`!_

Let's modify the following config values to the connector configuration `config.properties` and insert the values that
terraform logged before:

```properties
edc.vault.clientid=<client_id>
edc.vault.tenantid=<tenant_id>
edc.vault.certificate=<path_to_pfx_file>
edc.vault.name=<vault-name>
```

## Update data seeder

Put the storage account name into the DataAddress builder of the `FakeSetup` class.

```
DataAddress.Builder.newInstance()
   .type("AzureStorage")
   .property("account", "<storage-account-name>")
   .property("container", "src-container")
   .property("blobname", "test-document.txt")
   .keyName("<storage-account-name>-key1")
   .build();
```

## Take a look at the updated `transfer-file` and `api` module

For this chapter the file transfer extension (`CloudTransferExtension.java`) has been upgraded significantly. Most
notable are these changes:

- the extension now creates different catalog entries
- there are additional dependencies that take care of provisioning S3 buckets and reading blobs from Azure

Currently, we have implementations to _provision_ S3 buckets and Azure Storage accounts, but this example only contains
code to transfer data from Azure Storage to S3 (and not vice-versa). Check out the `*Reader.java` and `*Writer.java`
classes in the `transfer-file` module.

In the `api` module the `ConsumerApiController.java` has also been upgraded quite a bit. It now exposes endpoints to
start, check and deprovision transfer requests.

## Bringing it all together

While we have deployed several cloud resources in the previous chapter, the connectors themselves still run locally.
Thus, we can simply rebuild and run them:

```bash
./gradlew clean build
java -Dedc.fs.config=samples/05-file-transfer-cloud/consumer/config.properties -jar samples/05-file-transfer-cloud/consumer/build/libs/consumer.jar
# in another terminal window:
java -Dedc.fs.config=samples/05-file-transfer-cloud/provider/config.properties -jar samples/05-file-transfer-cloud/provider/build/libs/provider.jar
```

Once the connectors are up and running, we can initiate a data transfer by executing:

```bash
curl -X POST -H "Content-Type: application/json" -d @samples/05-file-transfer-cloud/datarequest.json "http://localhost:9191/api/datarequest"
```

like before that'll return a UUID. Using that UUID we can then query the status of the transfer process by executing:

```bash
curl -X GET "http://localhost:9191/api/datarequest/<UUID>/state"
```

which will return one of
the [TransferProcessStates](spi/src/main/java/org/eclipse/dataspaceconnector/spi/types/domain/transfer/TransferProcessStates.java)
enum values. Once the transfer process has reached the `COMPLETED` state, we can deprovision it using

```bash
curl -X DELETE http://localhost:9191/api/datarequest/<UUID>
```

Deprovisioning is not necessary per se, but it will do some cleanup, delete the temporary AWS role and the S3 bucket, so
it's generally advisable to do it.

### Alternative: IDS Multipart

The requesting of data offers and data transfer can also be initiated using IDS multipart.

#### 1. Request Data Offers

To request data offers from the provider run

```bash
curl -X GET -H 'X-Api-Key: password' http://localhost:9191/api/control/catalog?provider=http://localhost:8282/api/v1/ids/data
```

#### 2. Negotiate Contract

To negotiate a contract copy one of the contract offers into the statement below and execute it. At the time of writing
it is only possible to negotiate an _unchanged_ contract, so counter offers are not supported.

```bash
curl --location --request POST 'http://localhost:9191/api/control/negotiation' \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '{
  "type": "INITIAL",
  "protocol": "ids-multipart",
  "connectorId": "1",
  "connectorAddress": "http://localhost:8282/api/v1/ids/data",
  "correlationId": null,
  "contractOffer": { <Copy one of the contract offer from Step 2> },
  "asset": "1",
  "provider": "https://provider.com",
  "consumer": "https://consumer.com",
  "offerStart": null,
  "offerEnd": null,
  "contractStart": null,
  "contractEnd": null
}'
```

The EDC will answer with the contract negotiation id. This id will be used in step 4.

#### 3. Get Contract Agreement Id

To get the contract agreement id insert the negotiation id into the following statement end execute it.

```bash
curl --location --request GET 'http://localhost:9191/api/control/agreement/<NegotiationId>' \
--header 'X-API-Key: password'
```

The EDC will answer with the serialized contract negotiation. After the negotiation is done the serialized contract
negotiation will contain an agreement with id, that is required in the next step. This may take a few seconds.

#### 4. Transfer Data

To transfer data from the insert the contract agreement id from step 3 and execute the statement below.

```bash
curl --location --request POST 'http://localhost:9191/api/control/transfer' \
--header 'X-API-Key: password' \
--header 'Content-Type: application/json' \
--data-raw '
{
  "edctype": "dataspaceconnector:datarequest",
  "id": null,
  "processId": null,
  "connectorAddress": "http://localhost:8282/api/v1/ids/data",
  "protocol": "ids-multipart",
  "connectorId": "consumer",
  "assetId": "1",
  "contractId": "<ContractAgreementId>",
  "dataDestination": {
    "properties": {
      "region": "us-east-1"
    },
    "type": "AmazonS3"
  },
  "managedResources": true,
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  },
  "destinationType": "AmazonS3"
}'
```
