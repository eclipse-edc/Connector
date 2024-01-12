# Dynamic Constraint Functions

## Decision

A dynamic variant of the `AtomicConstraintFunction` will be introduced.

## Rationale

Currently, constraint functions can only be bound to a single key (or multiple keys if registered each key)
and the key has to be known at compile-time. The policy evaluation then works on an exact match of the key when evaluating
a `leftOperand` of a constraint.

We want to enable dynamic use cases where the final shape key it's not known at compile-time, or where users
can write their own matching/dispatching mechanism to the constraint functions.

## Approach

We will introduce a new interface `DynamicAtomicConstraintFunction`:

```java

public interface DynamicAtomicConstraintFunction<R extends Rule> {
    
    boolean evaluate(Object leftValue, Operator operator, Object rightValue, R rule, PolicyContext context);

    boolean canEvaluate(Object leftValue, Operator operator, Object rightValue, R rule, PolicyContext context);

}
```

Where the `canEvaluate` method decides if the constraints can be evaluated or not, based in the input parameters. 

The `PolicyEngine` interface will have an additional method for registering those dynamic functions.

```java
interface PolicyEngine {
    <R extends Rule> void registerFunction(String scope, Class<R> type, DynamicAtomicConstraintFunction<R> function);
}
```

Since constraints are evaluated also based on the `scope`, we will change the `RuleBindingRegistry`,
by adding a new method for registering dynamic binders:

```java
public interface RuleBindingRegistry {
    void registerDynamicBind(Function<String, Set<String>> binder);
}
```

Dynamic binders are functions that dynamically calculate the scopes for a `ruleType`.

When evaluating a constraint, the `PolicyEngine` should look up first for the exact match for a specific key binding,
if not found the dynamic functions will be invoked.