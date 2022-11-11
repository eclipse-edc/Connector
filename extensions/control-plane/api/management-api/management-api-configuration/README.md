# DataManagement API Configuration

This module provides central configuration for all DataManagement APIs, i.e. the `DataManagementApiConfiguration`, which
currently only contains the context alias, which all the Management API controllers should be registered under.

Further, this module registers an `AuthenticationRequestFilter` and an `EdcApiExceptionMapper` in the same context.

## Configurations

Exemplary configuration:

```properties
web.http.management.port=9191
web.http.management.path=/api/v1/management
web.http.port=8181
web.http.path=/api
```

Please note that since a specific context is registered (here: `data`), then the default one **must** be registered
explicitly!

_Caution: failing to provide a `web.http.management.port` and `web.http.management.path` configuration will cause
the `AuthenticationRequestFilter` and `EdcApiExceptionMapper` to be registered in the _default_ context causing it to
fire for EVERY request on that context including IDS communication._

## Authentication

If you want to secure the DataManagement API, you need to provide a module which implements the `AuthenticationService`
interface (e.g. `:extensions:common:auth:auth-tokenbased` or `:extensions:common:auth:auth-basic`).
