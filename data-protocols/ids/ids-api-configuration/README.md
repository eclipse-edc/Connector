# IDS API Configuration

This module provides a custom Jetty context for endpoints of the IDS API, thus allowing for custom
configuration of these endpoints. It creates and provides an 
[IdsApiConfiguration](./src/main/java/org/eclipse/dataspaceconnector/ids/api/configuration/IdsApiConfiguration.java),
which holds the context alias as well as the API path. This configuration can be used by other
extensions to register controllers in the IDS API context.

In the IDS API context, no authentication filters are registered, as verification of the requesting
party is done using `Dynamic Attribute Tokens`. There are also no exception mappers registered for
this context, since `RejectionMessages` with corresponding rejection reasons should be returned in
case of an exception.

If no configuration is provided for the IDS API context, the port will default to `8282` and the path will
default to `/api/v1/ids`.

### Configuration

In order to customize the IDS API context, provide the following configuration values and adjust
`web.http.ids.port` and `web.http.ids.path`:

```properties
web.http.port=8181
web.http.path=/api
web.http.ids.port=8282
web.http.ids.path=/api/v1/ids
```

Note, that the path and port for the default context have to be set explicitly as soon as a custom context
is specified.