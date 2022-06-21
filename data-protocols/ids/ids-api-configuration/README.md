# IDS API Configuration

This module provides a custom Jetty context for endpoints of the IDS API, thus allowing for custom
configuration of these endpoints. It creates and provides an 
[IdsApiConfiguration](./src/main/java/org/eclipse/dataspaceconnector/ids/api/configuration/IdsApiConfiguration.java),
which holds the context alias as well as the API path. This configuration can be used by other
extensions to register controllers in the IDS API context.

## Background

In the IDS API context, no authentication filters should be registered, as verification of the
requesting party is done using `Dynamic Attribute Tokens`. Also, there should be no exception mappers
registered for this context, since `RejectionMessages` with corresponding rejection reasons should
be returned in case of an exception. Therefore, it is necessary to separate IDS API endpoints from
all other API endpoints.

### Scope

This extension should be part of any connector that supports IDS messaging.

## Technical Details

### Interfaces

The extension adds an
[IdsApiConfiguration](src/main/java/org/eclipse/dataspaceconnector/ids/api/configuration/IdsApiConfiguration.java)
instance to the `ServiceExtensionContext`, which can be used to get information about the IDS API
configuration.

| Interface | Parameters | Description |
| :----| :---- | :-------------------- |
| IdsApiConfiguration.getContextAlias() | - | returns the alias under which the IDS API context is registered |
| IdsApiConfiguration.getPath() | - | returns the path at which endpoints of the IDS API are available |

### Dependencies

This module does not have any dependencies besides the `spi` module.

### Configurations

In order to customize the IDS API context, provide the following configuration values and adjust
`web.http.ids.port` and `web.http.ids.path`. Note, that the path and port for the default context
have to be set explicitly as soon as a custom context is specified.

```properties
web.http.port=8181
web.http.path=/api
web.http.ids.port=8282
web.http.ids.path=/api/v1/ids
```

If no configuration is provided for the IDS API context, the port will default to `8282` and the path will
default to `/api/v1/ids`.

## Decisions

As opposed to other API contexts, the IDS API context is always created, even if no corresponding 
configuration is supplied. This is due to the reasons described in [Background](#background).
