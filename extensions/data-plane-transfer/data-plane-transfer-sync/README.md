# Synchronous data plane transfer extension

## Overview

The `data-plane-transfer-sync` extension provides resources in order to perform synchronous data transfer using the Data
Plane. Data Plane exposes a public API enabling a consumer to actively query data using its own Date Plane. The access
to the public Data Plane API is secured through a bearer token authorization, whose generation is ensured by the current
extension. In order to keep the Data Plane stateless, it has been chosen to embed the address of the actual data source
as a string inside the claims of the bearer token. This information is encrypted to make it unreadable outside the Data
Plane tough. The bearer token also contains the contract id which is used when data are queried in order to check that
the consumer still has access to the data, i.e. contract between the two parties is still valid.

First part of the extension is a `DataFlowController` implementation called `ProviderDataPlaneProxyDataFlowController`
which is triggered when the destination type of the `DataRequest` is set to _HttpProxy_.

This data flow controller retrieves the actual data source using the `DataAddressResolver` and build a signed token
containing this data address(formatted as an encrypted string) as claim. This token is then put into
a `EndpointDataReference` (EDR) object along with the URL to the Data Plane public API from which data will be queried.

This EDR is then sent to the consumer Control Plane through the `RemoteMessageDispatcherRegistry`.

In the `data-protocols:ids` module an IDS `Sender` is registered (see
class `MultipartEndpointDataReferenceRequestSender`) that sends the EDR as an IDS `ParticipantUpdateNotification`
message. This message is then received by the `EndpointDataReferenceHandler` which:

- first apply the provided `EndpointDataReferenceTransformer` on the received EDR and generates a new EDR,
- then dispatch the resulting EDR by delegating to all `EndpointDataReferenceReceiver` registered in the registry.

## Validation API

Second part of the extension is an API which is used by the Data Plane in order to validate the bearer token (see
sections above) received in input of the public API request. This validation consists in:

- checking token signature,
- asserting that the token is still valid and that the Contract still allows access to the data.

If both conditions are satisfied then the data address from the claims is decrypted and returned back to the Data Plane
which will then perform the data transfer.