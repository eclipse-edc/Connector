# Perform a artifact download

This is similar to [04.0-file-transfer](../../../04.0-file-transfer/README.md) - the tasks are the same:

* [Same] Creating an additional connector, so that in the end we have two connectors, a consumer and a provider
* [Same] Providing communication between provider and consumer using IDS multipart messages
* [Same] Utilize Data management API to interact with connector system.
* [Same] Performing a contract negotiation between provider and consumer
* [Same] Performing a file transfer
  * [Same] The consumer will initiate a file transfer
  * [Different] The provider will fulfill that request by creating on the fly a presigned url for the cloud file and copy that file to the desired location. The presigned url expires so creating it in at the beginning could potentially lead to a bug due to unavailable resource.
  
## Transfer implementations

There are currently multiple implementation for the transfer available:
* Old implementation 
    - uses [org.eclipse.dataspaceconnector.api.control.ClientController](../../../../extensions/api/control/src/main/java/org/eclipse/dataspaceconnector/api/control/ClientController.java) for data transfer
    - endpoint /control/transfer
    - no JTA support
    - deprecated since 04/13/2022
* Custom implementation 
    - uses [com.siemens.mindsphere.datalake.edc.http.ConsumerApiController.initiateDataRequest](api/src/main/java/com/siemens/mindsphere/datalake/edc/http/ConsumerApiController.java)
    - endpoint /transfer/data/request
    - not standard
    - no multipart support
* Current implementation 
    - uses [org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.TransferProcessApiController.initiateTransfer](../../../../extensions/api/data-management/transferprocess/src/main/java/org/eclipse/dataspaceconnector/api/datamanagement/transferprocess/TransferProcessApiController.java)
    - endpoint /transferprocess
    - main difference from the deprecated control point is that the call to manager.initiateConsumerRequest is wrapped in a transaction context provided by [org.eclipse.dataspaceconnector.spi.transaction.TransactionContext](../../../../extensions/transaction/transaction-spi/src/main/java/org/eclipse/dataspaceconnector/spi/transaction/TransactionContext.java) thefore allowing JTA
    
Transfer process is done by a state machine:

![Transfer states](../../../../docs/diagrams/transfer-states.png)

Provisioning means:
* On a consumer, provisioning may entail setting up a data destination and supporting infrastructure
* On a provider, provisioning is initiated when a request is received and map involve preprocessing data or other operations
* The code [org.eclipse.dataspaceconnector.transfer.core.transfer.TransferProcessManagerImpl.processProvisioning](../../../../core/transfer/src/main/java/org/eclipse/dataspaceconnector/transfer/core/transfer/TransferProcessManagerImpl.java) is used for e.g. to provision the resource before transfer
* Examples for provioners supporting 
    - org.eclipse.dataspaceconnector.aws.s3.provision.AwsProvisionExtension
        - [org.eclipse.dataspaceconnector.aws.s3.provision.S3BucketProvisioner](../../../../extensions/aws/s3/s3-provision/src/main/java/org/eclipse/dataspaceconnector/aws/s3/provision/S3BucketProvisioner.java)
    - org.eclipse.dataspaceconnector.provision.azure.AzureProvisionExtension
        - [org.eclipse.dataspaceconnector.provision.azure.blob.ObjectStorageProvisioner](../../../../extensions/azure/blobstorage/blob-provision/src/main/java/org/eclipse/dataspaceconnector/provision/azure/blob/ObjectStorageProvisioner.java)
    - org.eclipse.dataspaceconnector.transfer.provision.http.HttpProvisionerExtension
        - [org.eclipse.dataspaceconnector.transfer.provision.http.impl.HttpProviderProvisioner](../../../../extensions/http-provisioner/src/main/java/org/eclipse/dataspaceconnector/transfer/provision/http/impl/HttpProviderProvisioner.java)
    - com.siemens.mindsphere.datalake.edc.http.DataLakeExtension
        - [com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlProvisioner](../../../../extensions/mindsphere/mindsphere-http/src/main/java/com/siemens/mindsphere/datalake/edc/http/provision/DestinationUrlProvisioner.java)

Process PROVISIONED transfer
* If CONSUMER, set it to REQUESTING, if PROVIDER initiate data transfer

Process REQUESTING transfer
* If CONSUMER, send request to the provider, should never be PROVIDER

Process REQUESTED transfer
* If is managed or there are provisioned resources set IN_PROGRESS or STREAMING, do nothing otherwise

Process IN PROGRESS transfer 
* if is completed or there's no checker and it's not managed, set to COMPLETE, nothing otherwise

Process DEPROVISIONING transfer
* Launch deprovision process. On completion, set to DEPROVISIONED if succeeded, ERROR otherwise
* This can be used to invalidate created presigned URLs on both consumer and provider for security reasons

    
## Sample call
    
![File Transfer HTTP MVP](mvp.png) 

## Relevant documentation

![Dataplane transfer client](../../../../docs/diagrams/data-plane-transfer/data-plane-transfer-client.png)

![Transfer states](../../../../docs/diagrams/data-plane-transfer/transfer-states.png)

![Eclipse DataSpace MVP](../../../../docs/diagrams/data-plane-transfer/mvp.png)

## Open API documentation

Open API doc is available at [openapi.md](../../../../openapi.md) and [resources/openapi/openapi.yaml](../../../../resources/openapi/openapi.yaml).
        
    