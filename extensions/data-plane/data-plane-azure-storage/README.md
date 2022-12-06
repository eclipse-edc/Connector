## Azure storage Data Plane module

### About this module

This module contains a Data Plane extension to copy data to and from Azure Blob storage.

When used as a source, it currently only supports copying a single blob.

The source `keyName` should reference a vault entry containing a storage [Shared Key](https://docs.microsoft.com/rest/api/storageservices/authorize-with-shared-key).

The destination `keyName` should reference a vault entry containing a JSON-serialized `AzureSasToken` object wrapping a [storage access signature](https://docs.microsoft.com/azure/storage/common/storage-sas-overview).
