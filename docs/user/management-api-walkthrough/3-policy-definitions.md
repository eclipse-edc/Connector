# Creating a Policy Definition

## Old plain JSON Schema

```json
{
  "id": "<POLICY-DEFINITION-ID>",
  "policy": {
    "permissions": [
      {
        "action": {
          "type": "USE"
        },
        "constraints": [
          {
            "leftExpression": {
              "value": "<LEFT-EXPRESSION-VALUE>"
            },
            "rightExpression": {
              "value": "<RIGHT-EXPRESSION-VALUE>"
            },
            "operator": "<OPERATOR>"
          }
        ]
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

## New JSON-LD Document

Policy model is now pure [ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl-model/) and going through it would help get a more complete picture.

> Please note: In our samples, except from `odrl` vocabulary terms that must override `edc` default prefixing, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@type":"PolicyDefinition",
  "@id": "<POLICY-DEFINITION-ID>",
  "policy": {
    "odrl:permission": [
      {
        "odrl:action": "USE",
        "odrl:constraint": [
          {
            "odrl:leftOperand": "<LEFT-OPERAND>",
            "odrl:operator": "<OPERATOR>",
            "odrl:rightOperand":  "<RIGHT-OPERAND>"
          }]
      }
    ],
    "odrl:prohibition": [],
    "odrl:obligation": []
  }
}
```

## Request

In this case we generate a very simple policy definition, that only contains the minimum in terms of information.
A Policy MUST have at least one permission, prohibition, or obligation property value of type Rule and in our case it will hold a permission defining our well-known `BusinessPartnerNumber` validation `Constraint`.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/policydefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "odrl": "http://www.w3.org/ns/odrl/2/"
              },
              "@type":"PolicyDefinition",
              "@id": "policy-definition-id",
              "policy": {
                "odrl:permission": [
                  {
                    "odrl:action": "USE",
                    "odrl:constraint": [
                      {
                        "odrl:leftOperand": "BusinessPartnerNumber",
                        "odrl:operator": "eq",
                        "odrl:rightOperand":  "BPN"
                      }]
                  }
                ],
                "odrl:prohibition": [],
                "odrl:obligation": []
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
