# EndToEnd Transfer Test

This tests verifies a complete scenario of contract negotiation and data transfer between a consumer and a provider with the HTTP Pull flow.

## Modules detail
* [backend-service](./backend-service): it represents an external application that interacts with the EDC.
* [control-plane](./control-plane): it's responsible for handling the contract negotiation phase
* [data-plane](./data-plane): it handles the data transfer phase acting as a proxy
* [runner](./runner): it contains the test implementation