# Azure Test

To run Azure Blob Integration tests you need to run an instance of [Azurite](https://docs.microsoft.com/azure/storage/common/storage-use-azurite) locally:
```
docker run -p 10000:10000 -e "AZURITE_ACCOUNTS=account1:key1;account2:key2" mcr.microsoft.com/azure-storage/azurite
```