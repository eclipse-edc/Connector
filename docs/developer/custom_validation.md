# Custom validation framework

The validation framework hooks into the normal Jetty/Jersey request dispatch mechanism and is designed to allow users to
intercept the request chain to perform additional validation tasks. In its current form it is intended for intercepting
REST requests. Users can elect any validation framework they desire, such as `jakarta.validation` or
the [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/), or they can implement one
themselves.

## When to use it

This feature is intended for use cases where the standard DTO validation, that ships with EDC's APIs is not sufficient.
Please check out the [OpenAPI spec](../../resources/openapi) to find out more about the object schema.

EDC features various data types that do not have a strict schema but are *extensible*, for example `Asset`/`AssetDto`,
or a `DataRequest`/`DataRequestDto`. This was done by design, to allow for maximum flexibility and openness. However,
users may still want to put a more rigid schema on top of those data types, for example a use case may require an
`Asset` to always have a `owner` property or may require a `contentType` to be present. The standard EDC validation
scheme has no way of enforcing that, so this is where _custom validation_ enters the playing field.

## Building blocks

There are two important components necessary for custom validation:

- the `InterceptorFunction`: a function that accepts the intercepted method's parameters as argument (as `Object[]`),
  and returns a `Result<Void>` to indicate the validation success. It **must not** throw an exception, or dispatch to
  the target resource is not guaranteed.
- the `ValidationFunctionRegistry`: all `InterceptorFunctions` must be registered there, using one of three registration
  methods (see below).

Custom validation works by supplying an `InterceptorFunction` to the `ValidationFunctionRegistry` in one of the
following ways:

1. bound to a resource-method: here, we register the `InterceptorFunction` to any of a controller's methods. That means,
   we need compile-time access to the controller class, because we use reflection to obtain the `Method`:
   ```java
   var method = YourController.class.getDeclaredMethods("theMethod", /*parameter types*/);
   var yourFunction = objects -> Result.success(); // you validation logic goes here
   registry.addFunction(method, yourFunction);
   ```
   Consequently `yourFunction` will get invoked before `YourController#theMethod` is invoked by the request dispatcher.
   Note that there is currently no way to bind an `InterceptorFunction` directly to an HTTP endpoint.

2. bound to an argument type: the interceptor function gets bound to all resource methods that have a particular type in
   their signature:
   ```java
   var yourFunction = objects -> Result.success(); // your validation logic goes here
   registry.addFunction(YourObjectDto.class, yourFunction);
   ```
   The above function would therefore get invoked in all controllers on the classpath, that have a `YourObjectDto`
   in their signature, e.g. `public void createObject(YourObjectDto dto)` and `public boolean deleteObject
   (YourObjectDto dto)` would both get intercepted, even if they are defined in different controller classes.
   *This is the recommended way in the situation described above - adding additional schema restrictions on extensible
   types*

3. globally, for all resource methods: this is intended for interceptor functions that should get invoked on *all*
   resource methods. *This is generally not recommended and should only be used in very specific situations such as
   logging*

Please check
out [this test](../../extensions/common/http/jersey-core/src/test/java/org/eclipse/edc/web/jersey/validation/integrationtest/ValidationIntegrationTest.java)
for a comprehensive example how validation can be enabled. All functions are registered during the extension's
initialization phase.

## Limitations and caveats

- `InterceptorFunction` objects **must not** throw exceptions
- all function registration must happen during the `initialize` phase of the extension lifecycle.
- interceptor functions **should not** perform time-consuming tasks, such as invoking other backend systems, so as not
  to cause timeouts in the request chain
- for method-based interception compile-time access to the resource is required. This might not be suitable for a lot of
  situations.
- returning a `Result.failure(...)` will result in an `HTTP 400 BAD REQUEST` status code. This is the only supported
  status code at this time. Note that the failure message will be part of the HTTP response body.
- binding methods directly to paths ("endpoints") is not supported.
