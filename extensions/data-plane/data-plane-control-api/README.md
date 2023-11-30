# Data Plane API

This extension provides the APIs of the Data Plane which are used by the Control Plane to delegate the data transfer 
to its Data Plane. APIs are not intended for public use. [See Data Plane Public API](../data-plane-public-api/README.md).

### Scope

These APIs have been designed to support the different data transfer types that the Data Plane must support, which are:

- Consumer Data Pull: the consumer backend applications use the Data Plane to proxy data request until the provided data source.
  This data transfer type is exclusively built around the [Data Plane Public API](../data-plane-public-api/README.md).
- Provider Push and Streaming: once contract is agreed by both participants, the Control Plane of the provider delegates
  the data transfer to its Data Plane through the Control API.

### Use Cases

#### Use Case #1

Provider wants to expose a Rest API taking as input some query parameters that restrict the amount of data returned for each query.
Here it is not feasible for the data consumer to negotiate a contract for each query that will hit the provider data source API.
Here the approach for the consumer would then be to
[negotiate with the provider the possibility to access the data source through a proxy](../../control-plane/data-plane-transfer/data-plane-transfer-sync/).
If the negotiation ends successfully, the consumer will be provided an access token that its backend applications can then use when querying the Data Plane public API.

This approach enables the consumer backend application to pass directly the query parameters, path parameters and body
in the request to the [Data Plane Public API](../data-plane-public-api/README.md). If the provider data source allows it, these parameters will then be conveyed until the data source.

### Use Case #2

Provider exposes some data located in a Cloud storage, such as AWS S3 or Azure storage. Consumer wants to copy these data
into its own storage system (which can be of a different type than the one of the provider). Here the consumer will negotiate a
_simple_ data transfer, by notifying to which location the data should be copied. Once contract is agreed between both parties,
the provider will automatically trigger the data transfer by delegating the data copy to is Data Plane system, through its Control API.

## Technical Details

### Interfaces

OpenApi documentation can be found [here](../../../resources/openapi/yaml/data-plane-control-api.yaml).

### Dependencies

_Provide some information about dependencies, e.g., used extensions._

| Name    | Description                                  |
|:--------|:---------------------------------------------|
| web-spi | Essentially for the Controllers registration |

## Design Principles

This API relies on the `DataPlaneManager` for executing the actual data transfer, see [Data Plane Framework](../../../core/data-plane/data-plane-framework/README.md) for more details.