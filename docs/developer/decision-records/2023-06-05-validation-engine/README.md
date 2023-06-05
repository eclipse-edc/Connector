# Validation engine

## Decision

We're removing Jakarta Validation engine and replace it with a self-implemented one.

## Rationale

With the support of JsonLD semantics, the Jakarta Validation framework became useless, because it relies on "Java Beans",
that we described with `Dto` classes, while we need a validation that needs to be applied after JsonLD expansion, so
it need to validate expanded `JsonObject` objects.

## Approach

We will follow a validator pattern, where a validator is an implementation of an interface like:
```
interface Validator<T> {
    ValidationResult validate(T input);
}
```

This way every `Controller` will need to have a `Validator` instance injected that will be applied on the expanded 
`JsonObject` and, if the validation fails, a `ValidationFailureException` will be thrown and then caught by a dedicated `ExceptionMapper`
that will return a 400 response with a detailed response body.

The `ValidationResult` should contain a list of `Violation`s, and every one of those should bring details about what's wrong
with the request and what's expected from the api.
