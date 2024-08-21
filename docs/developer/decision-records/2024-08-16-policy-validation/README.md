# Policy validation and evaluation plan

## Decision

The `PolicyEngine` will be extended in order to support:

- policies validation.
- evaluation plan of a `Policy` within a policy scope.

## Rationale

Currently, when creating a new `PolicyDefinition`, the only validation that takes place is on the shape of the
attached [ODRL](https://www.w3.org/TR/odrl-model/) policy.
For examples malformed `AtomicConstraint` rightOperands can be only detected at policy evaluation time, if any
evaluation function is invoked.
Also, any `leftOperand` that is not bound to a scope or an evaluation function within
a scope, may cause the `AtomicConstraint` to be filtered out on evaluation, making harder for providers to debug/verify
policy definitions.

## Approach

We will add for both `AtomicConstraintFunction` and `DynamicAtomicConstraintFunction` a default method for validating
the input of the `evaluate` method.

```java

//AtomicConstraintFunction
default Result<Void> validate(Operator operator, Object rightValue, R rule) {
    return Result.success();
}

//DynamicAtomicConstraintFunction
default Result<Void> validate(Object leftValue, Operator operator, Object rightValue, R rule) {
    return Result.success();
}
```

Then we will extend the `PolicyEngine` with two methods:

```java
interface PolicyEngine {
    Result<Void> validate(Policy policy);

    PolicyEvaluationPlan evaluationPlan(String scope, Policy policy);
}
```

### Validation

The `validate` methods validates the input policy using the current `PolicyEngine` configuration.

The validation phase should:

- Checks for each rule if the `action` is bound to at least one scope.
- Checks for each `AtomicConstraint` if the `leftOperand` is bound to at least one scope.
- For each `AtomicConstraint`, if the `leftOperand` is bound to an `AtomicConstraintFunction`, invoke
  the `validate` method.

### Evaluation Plan

Given a `Policy` and a policy scope, the evaluation plan will outline the evaluation steps that the policy engine
will do, without running the policy evaluation. It can be imagined as a sort of `EXPLAIN` equivalent of the SQL world.

The output will be the `PolicyEvaluationPlan`, which it's not defined in this DR, but it should contain useful
information like:

- Which pre-validators will be invoked
- Which policy functions will be invoked during the evaluation
- Which post-validators will be invoked
