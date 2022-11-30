# Control API Configuration

This module provides central configuration for all Control APIs, i.e. the `ControlApiConfiguration`, which
currently only contains the context alias, which all the Control API controllers should be registered under.

## Configurations

Exemplary configuration:

```properties
web.http.control.port=9191
web.http.control.path=/api/v1/control
```
