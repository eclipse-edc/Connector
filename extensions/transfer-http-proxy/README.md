# HTTP Transfers via Proxy

## Description
This extension is intended to show how _"consumer-pull"_ semantics could be implemented over HTTP. 
In this scenario, the consumer of a datatransfer requests an `Asset`, but instead of the provider kicking off the transfer asynchronously, it returns a URL and a token back to the consumer.
That URL refers to an API proxy in the provider's realm, which can be called using the token as bearer token. The API proxy forwards the consumer's HTTP request to the backend API, thus leveraging all of the connector's identity, authentication and authorization features on the one hand, and enabling massive scaling on the other hand - the API proxy could be a serverless function etc.

## Core changes
Core changes were made to these features:
- `TransferProcessManager`: was split into an `AsyncTransferProcessManager` and a `SyncTransferProcessManager`, the latter containing the proxying code.
- `TransferResponse`: can now carry data
- `ArtifactRequestController` and `DataRequestMessageSender`: they can now return and receive a `ProxyEntry` object respectively, which contains the URL and the token.

## State of this feature
Currently there are (almost no) tests, we've only tested this in demo installations due to time constraints we had. 

## Future developments
- dynamically spin up a HTTP proxy (serverless, managed api gateways)
- improve the token mechanism 
- add extensive testing
- improve module structure, move some code into separate modules.