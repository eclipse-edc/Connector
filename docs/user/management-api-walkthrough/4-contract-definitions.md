# Creating a Contract Definition

## Old plain JSON Schema

```json
{
  "id": "<CONTRACT-DEFINITION-ID>",
  "accessPolicyId": "<ACCESS-POLICY-ID>",
  "contractPolicyId": "<CONTRACT-POLICY-ID>",
  "assetsSelector": [
    {
      "operandLeft": "<OPERAND-LEFT>",
      "operator": "<OPERATOR>",
      "operandRight": "<OPERAND-RIGHT>"
    }
  ]
}
```

## New JSON-LD Document

> Please note: In our samples, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractDefinition",
  "@id": "<CONTRACT-DEFINITION-ID>",
  "accessPolicyId": "<ACCESS-POLICY-ID>",
  "contractPolicyId": "<CONTRACT-POLICY-ID>",
  "assetsSelector": [
    {
      "operandLeft": "<OPERAND-LEFT>",
      "operator": "<OPERATOR>",
      "operandRight": "<OPERAND-RIGHT>"
    }
  ]
}
```

## Request

In this case we generate a very simple contract definition, that only contains the minimum in terms of information.
A Contract Definition MUST have `accessPolicy`, `contractPolicy` identifiers and `assetsSelector`property values.
The `operandLeft` property value MUST contain the asset property full qualified term `<vocabulary-uri>/<term>`, in our case `https://w3id.org/edc/v0.0.1/ns/id`.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/contractdefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "@type": "ContractDefinition",
              "@id": "contract-definition-id",
              "accessPolicyId": "policy-id,
              "contractPolicyId": "policy-id",
              "assetsSelector": [
                {
                  "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
                  "operator": "=",
                  "operandRight": "asset-id"
                }
              ]
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
