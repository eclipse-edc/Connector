# Generic Properties

## Decision

All the `properties` field (like `privateProperties`, `extensibleProperties` without a predefined schema should be 
expressed by a `Map<String, Object>` signature.

## Rationale

These fields are meant to be as generic as possible to permit users to define their own type/structure.


## Approach

Define them as `Map<String, Object>`.

