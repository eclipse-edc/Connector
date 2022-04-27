## Azure storage Data Plane module

### About this module

This module contains a Data Plane extension to copy data to and from Azure Blob storage.

When used as a source, it currently only supports copying a single blob.

### Example usage

Create two Azure storage accounts with arbitrary names, here called ACCOUNT1 and ACCOUNT2.

In ACCOUNT1, create a storage container named `src`.

In ACCOUNT2, create a storage container named `dest`.

In ACCOUNT1, under storage container `src`, upload a file named `file.txt` with arbitrary content.

Run the Data Plane server:

```sh
env web.http.public.port=9191 web.http.control.path=/control ./gradlew :launchers:data-plane-server:run
```

In another terminal, send a data flow request, replacing ACCOUNT1 and ACCOUNT2 with the storage account names, and KEY1 and KEY2 with their respective access keys.

```sh
curl 'http://localhost:8181/control/transfer' \
--header 'Content-Type: application/json' \
--data '{
    "id": "B4819DE5-8B9F-4B44-8227-F37CF94744E9",
    "edctype": "dataspaceconnector:dataflowrequest",
    "processId": "6593E90E-DD13-4132-A6D0-ADEB02C32ECB",
    "sourceDataAddress": {
        "properties": {
            "type": "AzureStorage",
            "account": "ACCOUNT1",
            "container": "src",
            "blob": "file.txt",
            "sharedKey": "KEY1"
        }
    },
    "destinationDataAddress": {
        "properties": {
            "type": "AzureStorage",
            "account": "ACCOUNT2",
            "container": "dest",
            "sharedKey": "KEY2"
        }
    }
}'
```

The file `file.txt` is now copied into ACCOUNT2 under the container `dest`.
