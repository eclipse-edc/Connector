# Typed Policy Context

## Decision

We will permit the `PolicyEngine` to accept strongly typed `PolicyContext`s

## Rationale

At the moment implementing a new policy function for an adopter requires a "blind guess" about the content of the `PolicyContext`
object, because it's designed as an unstructured map.
Bounding the context structure to the scope will help documentation and usability of the Policy Engine.

## Approach


### Function interfaces

The refactor is based on the modification of the "policy function interfaces" to add them the `PolicyContext` generic type, to permit implementations
bound to the specific type.
For every of these 3 interfaces (`AtomicConstraintFunction`, `DynamicAtomicConstraintFunction`, `RuleFunction`) will be defined a new interface with the same signature,
plus the `C extends PolicyContext` generic type, e.g.:
```java
public interface AtomicConstraintRuleFunction<R extends Rule, C extends PolicyContext> {

    boolean evaluate(Operator operator, Object rightValue, R rule, C context);
    
    ...
}
```

The current interface will be deprecated and it will extend the new one setting `PolicyContext` as bound class. This will permit to avoid breaking changes:
```java
@Deprecated
public interface AtomicConstraintFunction<R extends Rule> extends AtomicConstraintRuleFunction<R, PolicyContext> { }
```

After then the current interfaces will be replaced by the new one in all the signature in the policy engine spi and implementation.

### Policy Engine

The change in the `PolicyEngine` is pretty straightforward: we need to add the generic type on the `evaluate` method:
```java
<C extends PolicyContext> Result<Void> evaluate(String scope, Policy policy, C context);
```

and add proper casting to the functions in the `PolicyEvaluator` build phase, e.g.:
```java
evalBuilder.dutyFunction(entry.key, (operator, value, duty) -> ((AtomicConstraintRuleFunction<Rule, C>) entry.function).evaluate(operator, value, duty, context));
```

The compiler will warn us that the cast is unchecked, there's not much to do about it, but as long as the functions are registered with the correct context on the scope,
there will be no issue, in fact, automated tests would be our shield against hard-to-understand runtime errors.

Tight binding between "scope" and the "context" is not in the scope of this PR, but it could be something to be achieved in the future.
Please note that is highly suggested to keep aligned the scope and the context hierarchy, e.g.:
- scope `foo` associated with `FooContext`
- scope `bar` associated with `BarContext` that extends `FooContext`

### Policy Contexts

The `PolicyContexts` extensions class and the scope constants will be kept in separated `spi` modules so then they could be used by different core and extension modules.
