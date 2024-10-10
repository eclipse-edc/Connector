# Typed Policy Scopes through Contexts

## Decision

We will bind the policy scope and the `PolicyContext` hierarchy.

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

The `PolicyEngine` will have new methods to register validators/function that accept also a `Class<PolicyContext>`. E.g.:
```java
<R extends Rule, C extends PolicyContext> void registerFunction(Class<C> contextType, Class<R> type, String key, AtomicConstraintRuleFunction<R, C> function);
```
Plus there will be a new `evaluate` method that will accept a typed context:
```java
<C extends PolicyContext> Result<Void> evaluate(Policy policy, C context);
```

the registered `contextType` object will then be used to filter out validators and functions during the evaluation, the validator/function
will be used only if the registered `contextType` `isAssignableFrom` the passed `context` class.
This means that they will be used only if the type is the same or a super type of the passed context, this will permit to
achieve scope inheritance, for example please consider:
- scope `foo` associated with `FooContext`
- scope `foo.bar` associated with `FooBarContext` that extends `FooContext`

In this case, when a `FooBarContext` object is passed to the `evaluate` function, will select also functions that were registered
on the `FooContext`.

### Policy Contexts

The `PolicyContexts` extensions class and the scope constants will be kept in separated `spi` modules so then they could be used by different core and extension modules.
