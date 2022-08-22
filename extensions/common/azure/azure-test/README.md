# Azure Test

## Blobstorage

To run Azure Blob Integration tests you need to run an instance of [Azurite](https://docs.microsoft.com/azure/storage/common/storage-use-azurite) locally:
```
docker run -p 10000:10000 -e "AZURITE_ACCOUNTS=account1:key1;account2:key2" mcr.microsoft.com/azure-storage/azurite
```

## CosmosDB

[CosmosDB Emulator](https://docs.microsoft.com/en-us/azure/cosmos-db/linux-emulator) is the tool you need for tests.

First, you need to export a variable with your ip address:
```
export IP_ADDRESS=$(ip addr | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}' | head -n 1 | cut -d/ -f1)
```

Then run the CosmosDB Emulator image:
```
docker run --rm -d -p 8081:8081 -p 10251:10251 -p 10252:10252 -p 10253:10253 -p 10254:10254 --name=test-linux-emulator \ 
    -e AZURE_COSMOS_EMULATOR_PARTITION_COUNT=6 -e AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE=$IP_ADDRESS \
    -it mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator
```