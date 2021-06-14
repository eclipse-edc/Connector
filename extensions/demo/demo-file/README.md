# File Transfer Example

The File Transfer Demo shows an example of a simple data exchange within the same connector and file system. 

| # | Connector | Step | Class |
| :---- | :----------- | :----------- | :----------- |
|1| Consumer |  Create new TransferProcess | DummyApiController |
|2| Consumer |  Provision Zip Archive | ZipArchiveProvisioner |
|3| Consumer |  Send IDS Remote Message to Provider (itself) | DataRequestMessageSender |
|4| Provider |  Receive IDS Artifact Request | ArtifactRequestController |
|5| Provider |  Create new TransferProcess | ArtifactRequestController | 
|6| Provider |  Initiate Data Flow | ZipPackDataFlowController |

> **Note**
> After the File Transfer Demo is started it creates some dummy data and registers them in the metadata store.
> The directory, where the files are created, must be writeable and is defined in the Configuration.java file.

## Getting Started

The Eclipse Dataspace Connector runs on port 8181 and is started with the command

``
./gradlew :distributions:file:run
``

## What's implemented

To create our file demo extension we implemented the following interfaces:

**Service Extension** interface registers our components within the Eclipse Dataspace Connector. The implementation *FileServiceExtension*
- generates the demo data in */tmp/catalog*
- registers the demo data in the metadata catalog
- registers the following classes in the EDC service locator
  - ZipArchiveResourceDefinitionGenerator
  - ZipArchiveProvisioner
  - ZipPackDataFlowController
    
**Resource Definition Generator** creates all the resources, that need to be provisioned. It is explicitly registered
within the consumer or provider process. The ZipArchiveResourceDefinitionGenerator decide depending on the destination type, 
whether an archive must be provisioned or not. If the destination type is *edc:zipArchive* it creates *ZipArchiveResourceDefinition*s,
which must be provisioned.

**Provisioner** interfaces implements the logic to provision and de-provision resourced, which were defined in the
*Resource Definition Generator*. For the provisioning of *ZipArchiveResourceDefinition* the provisioner ensures that an
empty archive exists, the *ZipPackDataFlowController* can use to write into.

**Data Flow Controller** initiates the data transfer between two connectors. For the implementation of the File Transfer Demo,
it was decided that the data flow should be from the provisioner to the consumer. But in theory it could both ways. The
*ZipPackDataFlowController* packs the requested file into the provided zip archive.

