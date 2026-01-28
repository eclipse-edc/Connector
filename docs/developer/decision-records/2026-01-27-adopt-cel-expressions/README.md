# Adopt CEL Expressions for Policy Evaluation

## Decision

The experimental work on Common Expression Language (CEL) for evaluating policy expressions will be adopted as a
first-class feature of EDC core. This means that users will be able to define and evaluate policies using CEL
expressions without requiring custom Java code for policy functions.

## Rationale

Integrating CEL into EDC core offers several advantages:

1. **User-Friendly Policy Definition**: CEL provides a more accessible way for users to define policies dynamically,
   reducing the need for Java coding skills.
2. **Flexibility**: CEL allows for more complex and nuanced policy definitions that can adapt to changing requirements
   without redeploying the EDC runtime.
3. **Performance**: CEL is designed for efficiency, ensuring that policy evaluations remain performant even with complex
   expressions.
4. **Standardization**: Adopting CEL aligns EDC with a widely recognized expression language, promoting consistency and
   interoperability.

## Approach

The original design document is
available [here](https://github.com/eclipse-edc/Virtual-Connector/blob/main/docs/common_expression_language.md) and
outlines the integration of CEL into EDC platform.

Here's the recap of the approach:

1. **CelExpression** Entity: Introduce a new entity to represent CEL expressions associated with atomic constraints.
2. **CelExpressionStore**: Implement a store to manage the persistence of CelExpression entities.
3. **CelExpressionService**: Create a service to handle the creation, retrieval, updating, and deletion of CelExpression
   entities.
4. **CelExpressionEngine**: Develop an engine that utilizes the CEL Java library to evaluate CEL expressions during
   policy evaluation.
5. **Policy Function Integration**: Implement a custom policy function that leverages the `CelExpressionEngine` for
   evaluating policies using CEL syntax.

The CEL based policy evaluation will be integrated alongside existing Java-based policy functions,
and will still be experimental until stabilized in future releases. It will not be included
in the default BOMs at first, and users will need to explicitly include the CEL extension to use this feature.

The Management API for CEL expressions won't be ported at first to EDC core, they will be ported after the stabilization
of the current `v4beta` release of the Management API.