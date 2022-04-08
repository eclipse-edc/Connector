# DataManagement API

This group of modules provides the DataManagement API. With this API you can manage different things of the connector.

## Authentication

The submodule `:extensions:api:data-management:api-configuration` **requires**, that an implementation of the
`AuthenticationService` interface was registered. Therefor you have to add an authentication module to your dependencies
(e.g. `:extensions:api:auth-tokenbased` or `:extensions:api:auth-basic`).