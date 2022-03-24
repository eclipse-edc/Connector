# Token Claim Validation Extension

Using the Token Claim Validation Extension it's possible to add configurable validation against `Participants` in
the `ContractDefinition.AccessPolicy`.

**Why only AccessPolicy?** Because if a custom validation would be used in the `ContractPolicy`, it would be necessary
to send it to the other connector. But nether is it possible to send a generic constraint to other connectors using IDS,
nor is it possible for another connector to enforce a configurable constraint reliable. Hence, the limit
to `AccessPolicy`.

A custom validation against a `Participant` claim can be done in three steps:

1. Include this extension
2. Add setting `edc.policy.validation.<constraint-name>.claim=<claim-name>`
3. Use the claim-validation in a `AtomicConstraint`.

`AtomicConstraint` example:

```json
{
  "leftExpression": {
    "value": "<constraint-name>"
  },
  "rightExpression": {
    "value": "<claim-value>"
  },
  "operator": "<EQ | IN | NEQ>"
}
```

**Operators**

- `EQ` is _true_ when the content of a claim is equal to the right value of the `Constraint`. If the claim is not part
  of the token or different it is _false_.
- `NEQ` is _true_ when the content of a claim is **not** equal to the right value of the `Constraint`. If the claim is
  not part of the token or equal it is _false_.
- `IN` is _true_ when the right value of the `Constraint` is contained in the content of a claim. If the claim is not
  part of the token or the right value of the `Constraint` is not part of the claim it is _false_.

**Please note** that this extension only supports `Constraints` that are part of a `Permission`.

## Example

### 1. Include the extension

#### Gradle KTS

Project
```bash
implementation(project(":extensions:policy:token-claim-validation"))
```

Package
```bash
implementation("org.eclipse.dataspaceconnector:policy-token-claim-validation:0.0.1-SNAPSHOT")
```

#### Maven

```xml

<dependency>
    <groupId>org.eclipse.dataspaceconnector</groupId>
    <artifactId>policy-token-claim-validation</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency> 
```

### 2. Add setting to the connector configuration

In this example the IDS Security Profile of the DAPS token should be validated.

This is how a DAPS token looks like:

```json
{
  "typ": "JWT",
  "kid": "default",
  "alg": "RS256"
}
.
{
  "scopes": [
    "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL"
  ],
  "aud": "idsc:IDS_CONNECTORS_ALL",
  "iss": "https://daps.aisec.fraunhofer.de",
  "nbf": 1632982369,
  "iat": 1632982369,
  "jti": "OTAyNTE1OTMzNTczMDgyMzUxNg==",
  "exp": 1632985969,
  "securityProfile": "idsc:TRUST_SECURITY_PROFILE",
  "referringConnector": "http://consumer-core.demo",
  "@type": "ids:DatPayload",
  "@context": "https://w3id.org/idsa/contexts/context.jsonld",
  "transportCertsSha256": "c15e6558088dbfef215a43d2507bbd124f44fb8facd561c14561a2c1a669d0e0",
  "sub": "A5:0C:A5:F0:84:D9:90:BB:BC:D9:57:3A:04:C8:7F:93:ED:97:A2:52:keyid:CB:8C:C7:B6:85:79:A8:23:A6:CB:15:AB:17:50:2F:E6:65:43:5D:E8"
}
.
<signature>
```

Create a new EDC validation function by adding this to the settings:

```bash
edc.policy.validation.ids-security-profile.claim=securityProfile
```

### 3. Use the custom constraint validation in a `ContractDefinition`

`ContractDefinition` as JSON (simplified):

```json
{
  "id": "98c9c8b4-532f-45d3-a737-7f5a56c15b3e",
  "accessPolicy": {
    "uid": "5cc32689-f258-44f6-8821-acf230804a41",
    "permissions": [
      {
        "target": "a4be1bbb-9610-4a2b-af29-50bb7e9a2fbc",
        "action": {
          "type": "USE"
        },
        "constraints": [
          {
            "leftExpression": {
              "value": "ids-security-profile"
            },
            "rightExpression": {
              "value": "idsc:TRUST_SECURITY_PROFILE"
            },
            "operator": "EQ"
          }
        ],
        "duties": []
      }
    ],
    "prohibitions": [],
    "obligations": []
  },
  "contractPolicy": {
  },
  "selectorExpression": {
    "criteria": []
  }
}
```
