# Http Dynamic EDR receiver

This extension is similar to the [HTTP EDR receiver](../transfer-pull-http-receiver). The difference is that the URL is not configured
at startup time, but instead the callback url is provided to the consumer connector when initiating a transfer request 
by passing a custom property in the transfer request payload e.g. The URL will be stored in the transfer process
and will be used by the consumer connector to dispatch the EDR

```json
{
  "edctype": "dataspaceconnector:datarequest",
  "protocol": "ids-multipart",
  "assetId": "test-document",
  "contractId": "1:8147d6d6-9734-4821-b93d-2832ea7892e4",
  "dataDestination": {
    "type": "HttpProxy"
  },
  "managedResources": false,
  "connectorAddress": "http://localhost:8282/api/v1/ids/data",
  "connectorId": "consumer",
  "properties": {
    "receiver.http.endpoint" : "http://localhost:9999"
  }
}
```

## Configuration

| Parameter name                        | Description                                                 | Mandatory | Default value |
|---------------------------------------|-------------------------------------------------------------|-----------|---------------|
| `edc.receiver.http.dynamic.endpoint`  | The fallbacke endpoint when the URL is missing from the TP. | false     | null          |
| `edc.receiver.http.dynamic.auth-key`  | The header name that will be sent with the EDR request.     | false     | null          |
| `edc.receiver.http.dynamic.auth-code` | The header value that will be sent with the EDR request.    | false     | null          |
