# Fetching provider's Catalog

## Old plain JSON Schema

```json
{
  "protocol" : "ids-protocol-http",
  "providerUrl": "<PROVIDER-URL>",
  "querySpec": {
    "offset": 0,
    "limit": 100,
    "filter": "",
    "range": {
      "from": 0,
      "to": 100
    },
    "sortField": "",
    "criterion": ""
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
  "protocol" : "dataspace-protocol-http",
  "providerUrl": "<PROVIDER-URL>",
  "querySpec": {
    "offset": 0,
    "limit": 100,
    "filterExpression": {
      "operandLeft": "<OPERAND-LEFT>",
      "operator": "<OPERATOR>",
      "operandRight": "<OPERAND-RIGHT>"
    }
  }
}
```

## Request

In this case we fetch a provider catalog.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/catalog/request" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "protocol" : "dataspace-protocol-http",
              "providerUrl": "http://provider-control-plane:8282/api/v1/dsp",
              "querySpec": {
                "offset": 0,
                "limit": 100
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
