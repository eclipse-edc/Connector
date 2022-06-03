# Jersey

This extension provides a `Jersey` implementation for the `WebService` service.

## Configuration

| Parameter name                        | Description                                                               | Default value                                 |
|---------------------------------------|---------------------------------------------------------------------------|-----------------------------------------------|
| `edc.web.rest.error.response.verbose` | If true, a detailed response body is sent to the client in case of errors | false                                         |
| `edc.web.rest.cors.enabled`           | Enables or disables the CORS filter                                       | false                                         |
| `edc.web.rest.cors.origins`           | Defines allowed origins for the CORS filter                               | "*"                                           |
| `edc.web.rest.cors.headers`           | Defines allowed headers for the CORS filter                               | "origin, content-type, accept, authorization" |
| `edc.web.rest.cors.methods`           | Defines allowed methods for the CORS filter                               | "GET, POST, DELETE, PUT, OPTIONS"             |