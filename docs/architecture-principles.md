#Architecture Key Principles

##I. Fail-fast and Explicit Configuration

1. Configuration should be loaded and validated at extension initialization so that issues are reported immediately. Do not lazy-load configuration unless it is required to do so.
2. Settings can be pulled from the extension context and placed into POJOs, which are passed to services via their constructor.
3. Service configuration requirements should always be explicit; as a general rule, do not pass a single configuration object with many values to multiple services.

##II. Errors
1. Each handler is responsible for creating their own error responses
2. Do not throw exceptions to signal a validation error; report the error (preferably collated) and return an error response.
3. Throw an unchecked exception if something unexpected happens (e.g. a backing store connection is down). Note that validation errors are expected.

##III. Simplicity
1. Avoid layers of indirection when they are not needed.
2. Do not overload the use of handlers (not in the OO method overloading sense).

###. Testing
1. All handlers and services should have dedicated unit tests with mocks used for dependencies.
2. When appropriate, prefer composing services via the constructor so that dependencies can be mocked as opposed to instantiating dependencies directly. 
