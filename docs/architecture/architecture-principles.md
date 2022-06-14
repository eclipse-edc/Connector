# Architecture Key Principles

## I. Fail-fast and Explicit Configuration

1. Configuration should be loaded and validated at extension initialization so that issues are reported immediately. Do not lazy-load configuration unless it is required to do so.
2. Settings can be pulled from the extension context and placed into configuration objects, which are passed to services via their constructor.
3. Service configuration requirements should always be explicit; as a general rule, do not pass a single configuration object with many values to multiple services.
   For example, see `HttpFunctionConfiguration.java`.
4. Annotate configuration keys with `EdcSetting` so that they may be tracked.

## II. Errors
1. Do not throw checked exceptions; use unchecked exceptions. If an unchecked exception type needs to be defined, inherit from EdcException.
2. Do not throw exceptions to signal a validation error; report the error (preferably collated) and return an error response.
3. Throw an unchecked exception if something unexpected happens (e.g. a backing store connection is down after a number of retries). Note that validation errors are expected.
   For example, see `Result.java`. 
4. Only throw an exception when there is no remediation possible, i.e. the exception is fatal. Do not throw an exception if an operation can be retried.  

## III. Simplicity
1. Avoid layers of indirection when they are not needed (e.g. "pass-through methods").

### IV. Testing
1. All handlers and services should have dedicated unit tests with mocks used for dependencies.
2. When appropriate, prefer composing services via the constructor so that dependencies can be mocked as opposed to instantiating dependencies directly, see `ProvisionManagerImpl`. 
   
### V. Data Objects
1. Do not create constructors that take many parameters; instead use the builder pattern. An example is `TransferProcess`.
2. Note that the motivation behind use of builders is not for immutability (although that may be good in certain circumstances). Rather, it is to make code less error-prone and 
   simpler given the lack of named arguments and optional parameters in Java.

### VI. Secrets
1. Only store secrets in the `Vault` and do not hold them in objects that may be persisted to other stores.
2. Do not log secrets or sensitive information.

### VII. Extensions and Libraries
1. Extension modules contribute a feature to the runtime such as a service. 
2. SPI modules define extensibility points in the runtime. There is a core SPI module that defines extensibility for essential runtime features. There are other SPI modules that 
   define extensibility points for optional features such as IDS.
3. Libraries are utility modules that provide classes which may be used by other modules. They do not directly contribute features to the runtime. 
4. An SPI module may only reference other SPI modules and library modules. 
5. An Extension module may only reference other SPI modules and library modules.
6. A library module may only reference other library modules.

### VIII. Build
1. There should only be a root `gradle.properties` that defines dependency versions. Do not create separate gradle.properties files in a module.
2. For external dependencies, do not reference the version directly. Instead, add an entry in `gradle.properties` so that it may be synchronized across the codebase.

### IX. Handling Null Return Values
1. In certain situations, `null` may need to be returned from a method, passed as a parameter, or set on a field. Only use `Optional` if a method is part of a fluent API. 
   Since the runtime will rarely require this, the project standard is to use the `org.jetbrains.annotations.Nullable` and `org.jetbrains.annotations.NotNull` annotations. 

### X. Objects Serialization/Deserialization
1. `TypeManager` is the component responsible for json ser/des, you can also use the `ObjectMapper` inside it, but there should be no other `ObjectMapper` instance.

### XI. Class Naming
1. A single implementor of an interface should be named `<interface name>Impl`.
2. An implementor who are meant to be the default implementation for an interface but other are/can be defined used instead.

### XII. Observability
1. Services are [instrumented for collecting essential metrics](../developer/metrics.md), in particular instances of `ExecutorService`.
