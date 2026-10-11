# Management API

This group of modules provides the Management API. With this API you can manage different things of the connector.

## Authentication

The submodule `:extensions:control-plane:api:management-api:management-api-configuration` **requires**, that an implementation of the
`AuthenticationService` interface was registered. Therefor you have to add an authentication module to your dependencies
(e.g. `:extensions:common:auth:auth-tokenbased` or `:extensions:common:auth:auth-basic`).

## Policy constraint right operands

A `rightOperand` is a literal: a non-blank string, a number, a boolean, a typed literal
(`{"@value": ..., "@type": ...}`) or an array of these. EDC applies no IRI semantics to it and constraint functions
compare values as given, so a value that identifies a resource is passed as a plain string.

- The Management API rejects a `{"@id": "..."}` right operand with `400 Bad Request`. This applies to v4 and v5 policy
  definitions (create and update) and to contract requests, wherever the constraint appears in the policy.
- A `{"@id": X}` right operand received over DSP is read as the string `X`.
- Right operands are always serialized as literals; `{"@id": ...}` is never emitted.

Numbers and booleans are passed as value objects, e.g. `{"@value": 5000}` or `{"@value": true}`.

Accepted:

```json
{
  "leftOperand": "https://w3id.org/example/region",
  "operator": "eq",
  "rightOperand": "https://w3id.org/example/region/EU"
}
```

Rejected with `400`:

```json
{
  "leftOperand": "https://w3id.org/example/region",
  "operator": "eq",
  "rightOperand": { "@id": "https://w3id.org/example/region/EU" }
}
```
