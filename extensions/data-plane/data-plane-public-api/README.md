# Data Plane API

This extension provides the interfaces and implementations for the Data Plane Public API which is essentially a data 
proxy enabling a consumer to actively query data out from the provider data source.

### Scope

See Data Plane scope [here](../data-plane-control-api/README.md#scope).

### Use Cases

See Data Plane use cases [here](../data-plane-control-api/README.md#use-cases).

## Technical Details

### Interfaces

OpenApi documentation can be found [here](../../../resources/openapi/yaml/data-plane-api.yaml).

### Dependencies

_Provide some information about dependencies, e.g., used extensions._

| Name    | Description                                  |
|:--------|:---------------------------------------------|
| web-spi | Essentially for the Controllers registration |

### Configurations

| Parameter name                                      | Description                                                                                       | Mandatory | Default value                          |
|:----------------------------------------------------|:--------------------------------------------------------------------------------------------------|:----------|:---------------------------------------|
| `edc.dataplane.token.validation.endpoint`  | Endpoint of the token validation server that will be hit when targeting the Data Plane public API | true      |                                        |

## Design Principles

This API relies on the `DataPlaneManager` for executing the actual data transfer, see [Data Plane Framework](../../../core/data-plane/data-plane-framework/README.md) for more details.

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
