# Custom DTO validation on REST APIs

## Decision

In addition to the default validation that is already in place for REST APIs, in particular their DTOs, we will
implement an extensible and easy way to provide custom validation functions.

## Rationale

As some of our APIs accept extensible type, i.e. types that don't have a rigid schema, such as `Asset`/`AssetDto`, users
may superimpose a custom schema on those types, thus requiring additional validation. For example, in some imaginary use
case there could be a business requirement, that every `Asset` must have a property called `owner`. The standard
validation we provide cannot consider that, so in that use case an additional validation is needed, that checks
the `owner` property for nullity, etc.

## Approach

Jersey offers a way to register one or more `InvocationHandler` objects that get called whenever a resource method is
invoked (i.e. a REST call happens). This can be done _per-method_, so a different `InvocationHandler` can be registered
for every REST endpoint.

Three different objects are needed for this:

- a `ResourceMethodInvocationHandlerProvider`: registers the `InvocationHandler`, either globally or per-method
- a `InvocationHandler`: gets called whenever the associated resource method is invoked. As this is a generic interface
  from the `java.lang.reflect` package, extra care must be taken when interpreting the method arguments to avoid class
  cast exceptions etc.
- an `AbstractBinder`: needs to be registered directly into Jersey's `ResourceConfig`

## Implementation considerations

### User requirements

As a user, who wants to add validation functionality, I want to:

- register a validation function (e.g. a lambda) globally. I do not want to think about which endpoint is affected.
- register a validation function (e.g. a lambda) for a particular type. Whenever there is a resource method, that has a
  particular type in its argument list, my validation function should get invoked.
- register a validation function (e.g. a lambda) for a particular resource method. I want to specify
  the `java.lang.reflect.Method` explicitly
- supply my custom messages whenever a validation fails

_Note: using this mechanism it is not possible to register an `InvocationHandler` directly for the endpoint path, e.
g. `/api/v1/data/asset`_

### User-facing SPI

I propose adding an interface called `CustomValidationRegistry` that could look roughly like the following:

```java
public interface CustomValidationRegistry {
    /**
     * Registers a validation function for a particular type (e.g. a DTO). The validation function gets applied to 
     * all resource methods that have a T object in their signature 
     * @param type The class of the object for which to register the function
     * @param interceptorFunction A function that evaluates the object and returns a Result
     */
    <T> void registerForType(Class<T> type, Function<T, Result> interceptorFunction);

    /**
     * Registers a validation function for all resource methods. Conditional evaluation must be done in the 
     * evaluation function itself
     * @param interceptorFunction Receives the list of arguments of the resource method, returns a Result
     */
    void register(Function<Object[], Result> interceptorFunction);

    /**
     * Registers a validation function for a particular resource method (= Controller method). The validation 
     * function only gets applied to that particular method.
     * @param method The {@link java.lang.reflect.Method} (of a controller) for which to register the function
     * @param interceptorFunction Receives the list of arguments of the resource method, returns a Result
     */
    void registerForMethod(Method method, Function<Object[], Result> interceptorFunction);
}
```

If the `interceptorFunction` returns a failed `Result`, the `InvocationHandler` will throw an
`InvalidRequestException`, resulting in an HTTP 400 error code. As a side note is important to wrap that exception in
an `InvocationTargetException`, so that it gets picked up by the method dispatcher.

Users can then `@Inject` this interface into their extension and register their validation functions. They are free to
use whatever validation mechanism the desire.

The `impl` for that registry would under the hood contain the glue code to perform the correct registrations with the
Jersey `ResourceConfig`.

**Restrictions**

- all validation functions must be registered _before_ the `start()` phase of the extension lifecycle
- all validation functions are considered immutable
- validation functions cannot throw an exception
- validation functions are evaluated _after_ the default bean validation.