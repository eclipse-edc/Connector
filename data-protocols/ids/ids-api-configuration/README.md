# IDS API Configuration

This module provides the possibility to use a custom Jetty context for endpoints of the IDS API,
thus allowing for custom configuration of these endpoints. It creates and provides an
[IdsApiConfiguration](./src/main/java/org/eclipse/dataspaceconnector/ids/api/configuration/IdsApiConfiguration.java),
which holds the context alias as well as the API path. This configuration can be used by other
extensions to register controllers in the IDS API context.

In the IDS API context, no authentication filters are registered, as verification of the requesting
party is done using `Dynamic Attribute Tokens`. There are also no exception mappers registered for
this context, since `RejectionMessages` with corresponding rejection reasons should be returned in
case of an exception.

If no configuration is provided for the IDS API context, the controllers will be registered in the
default API context.

### Configuration

In order to create and use a custom context, provide the following configuration values:

```properties
web.http.port=8181
web.http.path=/api
web.http.ids.port=8282
web.http.ids.path=/api/ids
```

Note, that the path and port for the default context have to be set explicitly as soon as a custom context
is specified.