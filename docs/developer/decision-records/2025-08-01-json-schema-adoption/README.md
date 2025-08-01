# JSON-schema Adoption for Management API

## Decision

Starting from `v4` of the Management API we will adopt JSON-schema for documentation and validation of the API payloads.

## Rationale

Since we use JSON-LD in management API as `JsonObject`, currently we document manually the Management API using
OpenAPI/Swagger annotations. This is error-prone and requires a lot of manual work to keep the documentation up to date.
By using JSON-schema, it will allow us to generate the OpenAPI documentation automatically in sync with the actual
schema of the API payloads.

We will also use JSON-schema to validate the API payloads of incoming requests. This means that that
starting from `v4` of the Management API, the validation will be done on the `compacted` JSON-LD representation of the
payloads instead of the `expanded` one.

EDC will still process the `expanded` JSON-LD representation of the payloads internally.

## Approach

At first a new extension will be created to host all the JSON-schema definitions for the Management API payloads, which
they will be published under the `https://w3id.org/edc/connector/management/schema/v4/` URL.

### Documentation

For documentation, we can just leverage the `@Schema` using the `ref` attribute pointing to the JSON-schema URL.

### Validation

The new extension will also register `Validator<JsonObject>` implementations based on JSON-schema for each type in the
`JsonObjectValidatorRegistry` using a key that carries the `version` and the `type`.

E.g. `v4:TransferProcess` for the `TransferProcess` type in the `v4` version of the Management API schema.

Additional validators can be still registered in the `JsonObjectValidatorRegistry` for specific type and version,
but the input `JsonObject` for the `Validator` will be in the request raw form which should be in the `compacted`
JSON-LD representation.