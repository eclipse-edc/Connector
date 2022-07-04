# Blob storage transfer

Decision record describing the Azure blob storage transfer end-to-end flow between two participants.  

This document describes the flow if the 2 participants are using Azure blob storage.

- A Provider connector that makes an asset available, and executes data transfer through its DPF service.
- A Consumer connector that requests the asset, and provides a destination Azure storage container.

## Description

The `data-plane-azure-storage` extension can be used on DPF to support blob transfers.

A client can trigger a blob transfer on the consumer side via the Data Management API.

The client needs to use the `managedResources=true` option in its API request. This option will make sure that the resources needed for the transfer (storage account) are created. [`managedResources=false` option for Azure storage transfer](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1241) is not supported yet. `managedResources=false` would be used if the client wants to use a pre-existing container without creating a new one.

If something goes wrong during the transfer, the Consumer would not be aware that an error occurred, it would just never see the transferProcess with completed state when polling for the result. There is an [EDC issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1242) to address this problem.

Storage accounts access key should be stored in respective Vaults (e.g. Azure Key Vault) before initiating transfers. The consumer provisions a new storage container and generates a write-only SAS token to allow the provider to write data to the consumer's container.

## Sequence diagram

The following sequence diagram describes the flow to transfer a blob from a provider storage account to a consumer storage account. As a prerequisite, contract negotiation must have been performed.

This sequence diagram describes the flow if the 2 participants are using Azure blob storage. If one of the 2 participant is using another type of storage, only half of the sequence diagram would reflect the reality.

The sequence starts from the client triggering the transfer on the consumer side and finishes when the consumer deprovisions its resources.

![blob-transfer](../../../architecture/data-transfer/diagrams/blob-transfer.png)

1. The client calls the data management API to trigger a transfer process. The requested asset is identified by the `assetId` and the `contractId` from previous contract negotiation. The client get the `PROCESS_ID` corresponding to the `transferProcess`. This `PROCESS_ID` will be used to get the transfer status. For now, `managedResources` needs to be set to true, to make sure that the consumer provisions the blob container. `managedResources=false` would be used if the client wants to use a pre-existing container without creating a new one, but this feature is not supported yet.  
2. Consumer gets the destination storage account access key in its Vault.  
3. Consumer creates a container where the Provider DPF may write blobs. The container is created only if the client specifies `managedResources=true`.
   The [ObjectStorageProvisioner](/extensions/azure/blobstorage/blob-provision/src/main/java/org/eclipse/dataspaceconnector/provision/azure/blob/ObjectStorageProvisioner.java) is responsible for provisioning the container and for generating a SAS token to access the container. 
   To generate a [SAS token](https://docs.microsoft.com/en-us/azure/storage/common/storage-sas-overview), a storage account key is needed. This storage account key should be stored and retrieved in the Consumer Vault.
4. Consumer stores the SAS token in its Vault.
5. Consumer sends an IDS message to the Provider, containing the information needed to transfer data to the destination container, including the asset id, the destination blob account and container name and the SAS token needed to write a blob to the container.  
6. Provider stores the SAS token in its Vault (rather than in the request to DPF, so that the latter may be persisted in a future implementation without containing secrets).
7. Provider initiates the blob transfer on the Provider DPF. The Provider DPF can be embedded or run in a separated runtime. If it runs on a separated runtime, the Provider's control plane initiates the transfer via an HTTP request.  
8. The Provider DPF gets the source storage account access key in the Provider Vault.  
9. The Provider DPF gets the SAS token needed to write the blob to the consumer blob container.  
10. The Provider DPF reads the data that needs to be transfered using an [AzureStorageDataSource](../../../../extensions/azure/data-plane/storage/src/main/java/org/eclipse/dataspaceconnector/azure/dataplane/azurestorage/pipeline/AzureStorageDataSource.java).  
11. The Provider DPF writes the data to the destination blob so that the consumer can access the data using an [AzureStorageDataSink](../../../../extensions/azure/data-plane/storage/src/main/java/org/eclipse/dataspaceconnector/azure/dataplane/azurestorage/pipeline/AzureStorageDataSink.java).
12. When the transfer is finished, the Provider DPF writes a blob called `.complete` to signal the completion. 
13. In the meantime, the consumer regularly checks if a blob named `.complete` exists in the container. Clients can poll the state using the `/transferprocess/<PROCESS_ID>/state` endpoint.
When the `.complete` is found, the consumer persists the new transferProcess state.  
14. The clients polls the transferProcess state.  
15. When the transfer is finished, the client can read the blob.  
16. Then, the client can call the Data Management API to deprovision the transfer process by the following two steps.
    - (17) Consumer deletes the container containing the blob. The [ObjectStorageProvisioner](../../../../extensions/azure/blobstorage/blob-provision/src/main/java/org/eclipse/dataspaceconnector/provision/azure/blob/ObjectStorageProvisioner.java) is responsible for deprovisioning the container.  
    - (18) Consumer deletes the SAS token in the Vault. The [TransferProcessManagerImpl](../../../../extensions/azure/blobstorage/blob-provision/src/main/java/org/eclipse/dataspaceconnector/transfer/core/transfer/TransferProcessManagerImpl.java) is responsible for deprovisioning the SAS token.
