# EDC Http Client

## Decision

An `EdcHttpClient` will be introduced to wrap `OkHttpClient` and `Failsafe` functionalities.

## Rationale

At the moment, everytime the `Failsafe` retry mechanism is needed there's the need to inject the relative `RetryPolicy` 
service and explicitly use it everytime we need to call an http endpoint.
Retry mechanism is something we definitely want to have out-of-the-box as in fact it is something we expect to see used
in every http call.

## Approach

An `EdcHttpClient` interface will be introduced in a `http-spi` module:
```java
public interface EdcHttpClient {

    Response execute(Request request) throws IOException;

    <T> CompletableFuture<T> executeAsync(Request request, Function<Response, T> mappingFunction);

    EdcHttpClient withDns(String dnsServer);

}
```

The implementation will use `okhttp` and `failsafe-okttp` to provide retry mechanism using the registered `RetryPolicy`.
The implementation service will be provided by the `CoreDefaultServicesExtension`.

This service would be used instead of `OkHttpClient` and `RetryPolicy`, and in the future it could provide other features
or, also, provide a more okhttp-agnostic interface.

The `withDns` method is necessary for the `WebDidExtension`.
