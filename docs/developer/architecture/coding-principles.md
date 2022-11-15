# Coding Principles and Style Guide

## I. Fail-fast and Explicit Configuration

1. Configuration should be loaded and validated at extension initialization so that issues are reported immediately. Do
   not lazy-load configuration unless it is required to do so.
2. Settings can be pulled from the extension context and placed into configuration objects, which are passed to services
   via their constructor.
3. Service configuration requirements should always be explicit; as a general rule, do not pass a single configuration
   object with many values to multiple services.
   For example, see `HttpFunctionConfiguration.java`.
4. Annotate configuration keys with `@Setting` so that they may be tracked.

## II. Errors

1. Do not throw checked exceptions; use unchecked exceptions. If an unchecked exception type needs to be defined,
   inherit from EdcException.
2. Do not throw exceptions to signal a validation error; report the error (preferably collated) and return an error
   response.
3. Throw an unchecked exception if something unexpected happens (e.g. a backing store connection is down after a number
   of retries). Note that validation errors are expected.
   For example, see `Result.java`.
4. Only throw an exception when there is no remediation possible, i.e. the exception is fatal. Do not throw an exception
   if an operation can be retried.

## III. Simplicity

1. Avoid layers of indirection when they are not needed (e.g. "pass-through methods").
2. Avoid needlessly wrapping objects, especially primitive datatypes.

## IV. General Coding Style

1. Use `var` instead of explicit types (helps with clarity)
2. Avoid `final` in method args and local variables
3. Use `final` in field declarations
4. Avoid `static` fields except in constants or when absolutely necessary. (you should be able to provide a reason).
5. Use interfaces to define shared constants
6. Use "minimally required types" (or "smallest possible API"), e.g. use `ObjectMapper` instead of `TypeManager`
   , or use a `String` instead of a more complex object containing the String, etc.
7. Use either `public` members, which are documented and tested, or `private` members.
8. Avoid package-private members, especially if only needed for testing
9. Avoid `protected` members unless they're intended to be overridden.
10. Use package-private classes if they're not needed outside the package, e.g. implementation classes
11. Avoid using `enum`s for anything other than named integer enumerations.
12. Avoid using static classes as much as possible. Exceptions to this are helper functions and test utils, etc. as well
    as static inner classes.
13. Use only camel case and no prefixes for naming.
14. Avoid unnecessary `this.` except when it is necessary e.g. when there is a name overlap
15. Use static imports, as long as code readability and comprehension is not impacted. For example,
    - use `assertThat(...)` instead of `Assertions.assertThat(...)`
    - use `format("...",arg1)` instead of `String.format(...)`, but
    - avoid `of(item1, item2).map(it -> it.someOperation)...` instead of `Stream.of(item1, item2)`.
      Also, avoid static imports if two static methods with the same name would be imported from different classes
16. Avoid `Optional` as method return type or method argument, except when designing a fluent API. Use `null` in
    signatures.
17. Avoid cryptic variable names, especially in long methods. Instead, try to write them out, at least to a reasonable
    extent.

## V. Testing

1. All handlers and services should have dedicated unit tests with mocks used for dependencies.
2. Prefer unit tests over all other test types: unit > integration/component > e2e
3. When appropriate, prefer composing services via the constructor so that dependencies can be mocked as opposed to
   instantiating dependencies directly.
4. Use classes with static test functions to provide common helper methods, e.g. to instantiate an object.
5. Use `[METHOD]_when[CONDITION]_should[EXPECTATION]` as naming template for test methods,
   e.g. `verifyInput_whenNull_shouldThrowNpe()` as opposed to `testInputNull()`

## VI. Data Objects

1. Use the `Builder` pattern when:
    - there are any number of optional constructor args
    - there are more than 3 constructor args
    - inheriting from an object that fulfills any of the above. In this case use derived builders as well.

2. Although serializability is not the reason we use the builder pattern, it is a strong indication that a builder
   should be used.
2. Builders should be named just `Builder` and be static nested classes.
3. Create a `public static Builder newInstance(){...}` method to instantiate the builder
4. Builders have non-public constructors
5. Use single-field builders: a `Builder` instantiates the object it builds in its constructor, and sets the properties
   in its builder methods. The `build()` method then only performs verification (optional) and returns the instance.
6. Use `private` constructors for the objects that the builder builds.
7. If there is a builder for an object, use it to deserialize an object, i.e. put Jackson annotations such
   as `JsonCreator` and `@JsonBuilder` on builders.
8. Note that the motivation behind use of builders is not for immutability (although that may be good in certain
   circumstances). Rather, it is to make code less error-prone and
   simpler given the lack of named arguments and optional parameters in Java.

## VII. Secrets

1. Only store secrets in the `Vault` and do not hold them in objects that may be persisted to other stores.
2. Do not log secrets or sensitive information.

## VIII. Extensions and Libraries

1. Extension modules contribute a feature to the runtime such as a service.
2. SPI modules define extensibility points in the runtime. There is a core SPI module that defines extensibility for
   essential runtime features. There are other SPI modules that
   define extensibility points for optional features such as IDS.
3. Libraries are utility modules that provide classes which may be used by other modules. They do not directly
   contribute features to the runtime.
4. An SPI module may only reference other SPI modules and library modules.
5. An Extension module may only reference other SPI modules and library modules.
6. A library module may only reference other library modules.

## IX. Build

1. There should only be a root `gradle.properties` that contains build variables. Do not create separate
   `gradle.properties` files in a module.
2. For external dependencies, do not reference the version directly. Instead, use
   the [version catalog](../version-catalogs.md) feature.

## X. Handling Null Return Values

1. In certain situations, `null` may need to be returned from a method, passed as a parameter, or set on a field. Only
   use `Optional` if a method is part of a fluent API.
   Since the runtime will rarely require this, the project standard is to use the `org.jetbrains.annotations.Nullable`
   and `org.jetbrains.annotations.NotNull` annotations.

## XI. Objects Serialization/Deserialization

1. `TypeManager` is the component responsible for json ser/des, you can also use the `ObjectMapper` inside it, but there
   should be no other `ObjectMapper` instance.

## XII. Class Naming

1. A single implementor of an interface should be named `<interface name>Impl`.
2. An implementor who are meant to be the default implementation for an interface but other are/can be defined used
   instead.

## XIII. Observability

1. Services are [instrumented for collecting essential metrics](../metrics.md), in particular instances
   of `ExecutorService`.

## XIV. Streams

1. Always close explicitly `Stream` objects that are returned by a service/store, since they could carry a connection,
   and otherwise it will leak.
