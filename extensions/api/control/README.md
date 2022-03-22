**Please note**

### Work in progress

All content reflects the current state of discussion, not final decisions.

---

# Client API Extension

## Configuration

| Key |  Description | Default |
|:---|:---|:---|
| edc.api.control.auth.apikey.key | The HTTP headers name carrying the API key |`X-API-KEY`|
| edc.api.control.auth.apikey.value | The API-Key expected to be present on incoming HTTP requests | *random value generated during boot time*

## Initiate Data Transfer

To initiate the data transfer send a data request to the API endpoint.

### Example

Create a `request.json` file and add the content below.

```json
{
  "edctype": "dataspaceconnector:datarequest",
  "id": null,
  "processId": null,
  "connectorAddress": "http://localhost:8181/api/v1/ids/data",
  "protocol": "ids-multipart",
  "connectorId": "consumer",
  "assetId": "1",
  "contractId": "1",
  "dataDestination": {
    "properties": {
      "container": "consumer",
      "keyName": "consumer-blob-storage-key",
      "type": "AzureStorage",
      "account": "edc101",
      "blobname": "received.txt"
    },
    "keyName": "consumer-blob-storage-key",
    "type": "AzureStorage"
  },
  "managedResources": true,
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  },
  "destinationType": "AzureStorage"
}
```

Send the following CURL command using the command line. Adjust the content of the request and the control URL as
required.

`curl -X POST -H "Content-Type: application/json" -d @request.json http://localhost:8181/api/control/transfer`
