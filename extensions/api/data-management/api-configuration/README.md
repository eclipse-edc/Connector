This module provides central configuration for all DataManagement APIs, i.e. the `DataManagementApiConfiguration`, which
currently only contains the context alias, which all the Data Management API controllers should be registered under.

Further, this module registers an `AuthenticationRequestFilter` and an `EdcApiExceptionMapper` in the same context.

Exemplary configuration:

```properties
web.http.data.port=9191
web.http.data.path=/api/v1/data
web.http.port=8181
web.http.path=/api
```

Please note that since a specific context is registered (here: `data`), then the default one must be registered
explicitly!

_Caution: failing to provide a `web.http.data.port` and `web.http.data.path` configuration will cause
the `AuthenticationRequestFilter` and `EdcApiExceptionMapper` to be registered in the _default_ context causing it to
fire for EVERY request on that context including IDS communication._