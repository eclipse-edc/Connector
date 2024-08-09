# Transfer Data Plane

This extension provides services for delegating data transfer to the Data Plane. Especially two types of data transfers are supported:

- Consumer Pull: data consumer pulls actively the data by hitting an endpoint exposed by the provider. This use-case is typically
  used in cases where the data provider wants to use its Data Plane as a http proxy for querying data to an API.
- Provider Push: if data request is successfully processed, then the provider pushes data from its data source to the consumer.

## Background

The Data Plane is the component performing the actual data exchange between the provider and the consumer. Once an agreement
is established between the consumer and the provider, then the consumer can trigger the data request that will initiate the data exchange.
When at this stage, the present extension is used for delegating the data transfer to the appropriate Data Plane instance.

### Scope

This extension is to be used for every data transfer use-case relying on the EDC data plane.

### Use Cases

#### Consumer pull

Let us consider that a data provider wants to expose a REST API serving flight schedule data. This REST API takes in input
a set of query parameters which enables to restrict the amount of data returned in the response, e.g. departure date, boarding airport...
A consumer of this API will potentially be interested in hitting this API several times, potentially with different parameters, depending
on how the consumer backend application are exploiting the data.

For this use-case, the Provider Push data transfer type would not be relevant, as it would require to establish a new contract before
the consumer send a new http request, which would potentially overload the Control Plane. The Consumer Pull data transfer comes handy in that case.

To trigger this data transfer type, the destination type of the `DataRequest` must be set to `HttpProxy`.

#### Provider push

A consumer wants to perform a one-time transfer of a large amount of data stored in a S3 bucket on provider side, and
put these data into its Azure Blob Storage.

## Technical Details

### Interfaces

This extension introduces a Control Plane endpoint used by the Data Plane for validating the access token received in input
of its public API. OpenApi documentation can be found [here](../../../../resources/openapi/yaml/transfer-data-plane.yaml).
