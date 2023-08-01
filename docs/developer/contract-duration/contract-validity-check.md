# Contract In Force Policy Specification

This document defines an interoperable policy for specifying in force periods for contract agreements. An in force
period can be defined as a __duration__ or a __fixed date__.
All dates must be expressed as UTC.

## 1. Duration

A duration is a period of time starting from an offset. EDC defines a simple expression language for specifying the
offset and duration in time units:

```<offset> + <numeric value>ms|s|m|h|d```

The following values are supported for `<offset>`:

| Value             | Description                                                                                                                           |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| contractAgreement | The start of the contract agreement defined as the timestamp when the provider enters the AGREED state expressed in UTC epoch seconds |

The following values are supported for the time unit:

| Value | Description  |
|-------|--------------|
| ms    | milliseconds |
| s     | seconds      |
| m     | minutes      |
| h     | hours        |
| d     | days         |

A duration is defined in a `ContractDefinition` using the following policy and left-hand
operands `https://w3id.org/edc/v0.0.1/ns/inForceDate`:

```json
{
  "@context": {
    "cx": "https://w3id.org/cx/v0.8/",
    "@vocab": "http://www.w3.org/ns/odrl.jsonld"
  },
  "@type": "Offer",
  "@id": "a343fcbf-99fc-4ce8-8e9b-148c97605aab",
  "permission": [
    {
      "action": "use",
      "constraint": {
        "and": [
          {
            "leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
            "operator": "gte",
            "rightOperand": {
              "@value": "contractAgreement",
              "@type": "https://w3id.org/edc/v0.0.1/ns/inForceDate:dateExpression"
            }
          },
          {
            "leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate:inForceDate",
            "operator": "lte",
            "rightOperand": {
              "@value": "contractAgreement + 100d",
              "@type": "https://w3id.org/edc/v0.0.1/ns/inForceDate:dateExpression"
            }
          }
        ]
      }
    }
  ]
}
```

## 2. Fixed Date

Fixed dates may also be specified as follows using `https://w3id.org/edc/v0.0.1/ns/inForceDate` operands:

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
    "@vocab": "http://www.w3.org/ns/odrl.jsonld"
  },
  "@type": "Offer",
  "@id": "a343fcbf-99fc-4ce8-8e9b-148c97605aab",
  "permission": [
    {
      "action": "use",
      "constraint": {
        "and": [
          {
            "leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
            "operator": "gte",
            "rightOperand": {
              "@value": "2023-01-01T00:00:01Z",
              "@type": "xsd:datetime"
            }
          },
          {
            "leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
            "operator": "lte",
            "rightOperand": {
              "@value": "2024-01-01T00:00:01Z",
              "@type": "xsd:datetime"
            }
          }
        ]
      }
    }
  ]
}
```

Although `xsd:datatime` supports specifying timezones, UTC should be used. It is an error to use an `xsd:datetime`
without specifying the timezone.

## 3. No Period

If no period is specified the contract agreement is interpreted as having an indefinite in force period and will remain
valid until its other constraints evaluate to false.

## 4. Not Before and Until

`Not Before` and `Until` semantics can be defined by specifying a single `https://w3id.org/edc/v0.0.1/ns/inForceDate`
fixed date constraint and an
appropriate operand. For example, the following policy
defines a contact is not in force before `January 1, 2023`:

 ```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "@vocab": "http://www.w3.org/ns/odrl.jsonld"
  },
  "@type": "Offer",
  "@id": "a343fcbf-99fc-4ce8-8e9b-148c97605aab",
  "permission": [
    {
      "action": "use",
      "constraint": {
        "leftOperand": "edc:inForceDate",
        "operator": "gte",
        "rightOperand": {
          "@value": "2023-01-01T00:00:01Z",
          "@type": "xsd:datetime"
        }
      }
    }
  ]
}
```

## 5. Examples

- In-force policy with a fixed validity: [policy.inforce.fixed.json](./policy.inforce.fixed.json)
- In-force policy with a relative validity duration: [policy.inforce.duration.json](./policy.inforce.duration.json)

_Please note that the samples use the abbreviated prefix notation `"edc:inForceDate"` instead of the full
namespace `"https://w3id.org/edc/v0.0.1/ns/inForceDate"`._
