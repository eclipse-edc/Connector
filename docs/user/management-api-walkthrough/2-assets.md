# Creating an Asset

## Old plain JSON Schema

```json
{
  "asset": {
    "id": "<ASSET-ID>",
    "properties": {
      "name": "<ASSET-NAME>",
      "description": "<ASSET-DESCRIPTION>",
      "version": "<ASSET-VERSION>",
      "contenttype": "<ASSET-CONTENT-TYPE>"
    }
  },
  "dataAddress": {
    "properties": {
      "type": "<SUPPORTED-TYPE>"
    }
  }
}
```

## New JSON-LD Document

> Please note: In our samples, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "Asset",
  "@id": "<ASSET-ID>",
  "properties": {
    "name": "<ASSET-NAME>",
    "description": "<ASSET-DESCRIPTION>",
    "version": "<ASSET-VERSION>",
    "contenttype": "<ASSET-CONTENT-TYPE>"
  },
  "privateProperties": {
    "private-property": "<PRIVATE-PROPERTY-VALUE>"
  },
  "dataAddress": {
    "type": "<SUPPORTED-TYPE>"
  }
}
```

A new addition are the `privateProperties`.
Private properties will not be sent through the dataplane and are only accessible via the management API.
This enables the storage of additional information pertaining the asset, that is not relevant for the consumer, but is nonetheless useful for the provider.
Private properties are stores inside the `privateProperties` field.

> Please note:
> `privateProperties` are entirely optional and the field is not required for creating or updating an asset.
> `dataAddress` should correspond to one of the supported types by the connector, e.g. HttpData and AmazonS3, and it should include all the necessary properties associated with the chosen type.

## Request

In this case we generate a very simple asset, that only contains the minimum in terms of information.
For this we need both an asset and a data address, which together form an asset entry.

```bash
curl -X POST "${MANAGEMENT_URL}/v3/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "@type": "Asset",
              "@id": "asset-id"
              "dataAddress": {
                "type": "HttpData",
                "baseUrl": "https://jsonplaceholder.typicode.com/todos"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
