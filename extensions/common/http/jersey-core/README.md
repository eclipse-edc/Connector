# Jersey

This extension provides a `Jersey` implementation for the `WebService` service.

## Configuration

| Parameter name                        | Description                                      | Default value                                 |
|---------------------------------------|--------------------------------------------------|-----------------------------------------------|
| `edc.web.rest.cors.enabled`           | Enables or disables the CORS filter              | false                                         |
| `edc.web.rest.cors.origins`           | Defines allowed origins for the CORS filter      | "*"                                           |
| `edc.web.rest.cors.headers`           | Defines allowed headers for the CORS filter      | "origin, content-type, accept, authorization" |
| `edc.web.rest.cors.methods`           | Defines allowed methods for the CORS filter      | "GET, POST, DELETE, PUT, OPTIONS"             |

## Exception handling

The `JerseyService` provide 3 exception mappers:
 - `EdcApiExceptionMapper`: catches all the `EdcApiException` exceptions, map them to a 4xx response code with a detailed response body
 - `ValidationExceptionMapper`: catches all the `ConstraintViolationException` exceptions thrown by the `jersey-bean-validation` module, it maps them to a 400 response code with a detailed response body
 - Exception mapper that catches all the exceptions that are not handled by the other mappers, for example:
   - all the `java.lang` exceptions
   - jakarta.ws.rs.WebApplicationException and subclasses