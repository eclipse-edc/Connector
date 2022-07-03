# Data Plane API

This extension provides the interfaces and implementations for the Data Plane API, which are respectively:

- the Control API which is used by the Control Plane to delegate the data transfer to its Data Plane,
- the Public API which is essentially a data proxy enabling a consumer to actively query data out from the provider data source.

### Scope

These APIs have been designed to support the different data transfer types that the Data Plane must support, which are:

- Consumer Data Pull: the consumer backend applications use the Data Plane to proxy data request until the provided data source.
  This data transfer type is exclusively built around the Public API of the Data Plane.
- Provider Push and Streaming: once contract is agreed by both participants, the Control Plane of the provider delegates
  the data transfer to its Data Plane through the Control API.

### Use Cases

#### Use Case #1

Provider wants to expose a Rest API taking as input some query parameters that restrict the amount of data returned for each query.
Here it is not feasible for the data consumer to negotiate a contract for each query that will hit the provider data source API.
Here the approach for the consumer would then be to
[negotiate with the provider the possibility to access the data source through a proxy](../../data-plane-transfer/data-plane-transfer-sync).
If the negotiation ends successfully, the consumer will be provided an access token that its backend applications can then use when querying the Data Plane public API.

This approach enables the consumer backend application to pass directly the query parameters, path parameters and body
in the request to Data Plane public API. If the provider data source allows it, these parameters will then be conveyed until the data source.

### Use Case #2

Provider exposes some data located in a Cloud storage, such as AWS S3 or Azure storage. Consumer wants to copy these data
into its own storage system (which can be of a different type than the one of the provider). Here the consumer will negotiate a
_simple_ data transfer, by notifying to which location the data should be copied. Once contract is agreed between both parties,
the provider will automatically trigger the data transfer by delegating the data copy to is Data Plane system, through its Control API.

## Technical Details

### Interfaces

OpenApi documentation can be found [here](../../../resources/openapi/yaml/data-plane-api.yaml).

### Dependencies

_Provide some information about dependencies, e.g., used extensions._

| Name        | Description                                  |
|:------------|:---------------------------------------------|
| spi:web-spi | Essentially for the Controllers registration |

### Configurations

| Parameter name                                      | Description                                                                                       | Mandatory | Default value                          |
|:----------------------------------------------------|:--------------------------------------------------------------------------------------------------|:----------|:---------------------------------------|
| `edc.dataplane.token.validation.endpoint`  | Endpoint of the token validation server that will be hit when targeting the Data Plane public API | true      |                                        |

## Design Principles

Both public and control APIs rely on the `DataPlaneManager` for executing the actual data transfer, see [Data Plane Framework](../data-plane-framework/README.md) for more details.

The Data Plane public API takes an access token in input from the `Authorization` header, which is validated and decode by calling the
validation server. If the validation is successful, then the Data Plane is executed in order to query the data from the data address returned by the validation server.
As mentioned earlier, the Data Plane public API is essentially a data proxy, which by definition will convey the information from the request (query parameters, path information, request body)
into the `DataPlaneManager` through the `DataFlowRequest` properties. If the data source allows it, then the request information will
be used in the request to the actual data source.

### Security

Data Plane public API is, by nature, opened to external incoming http calls. Because it accepts any set of query params, path parameters and request body,
it could be used by malicious attackers to attempt an attack e.g. DDoS.

By default, Jetty, which is the embedded HTTP server in the EDC, provides some security against this:

- max request header size of 8 KB: every call whose request header size is larger is discarded,
- max form content size of 200 KB: every form request call whose body is larger than this is discarded,
- max form content keys of 1000: every form request call which contains more keys is discarded.

No other limits are implemented as of now, so we strongly recommend putting a reverse proxy in front of the Data Plane public API, such as Nginx, configured to block malicious calls.