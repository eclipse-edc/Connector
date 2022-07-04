# Data Plane HTTP extension

## Background

### Scope

This extension provides support for sending data sourced from an HTTP endpoint and posting data to an HTTP endpoint. By
nature of the DPF design, which supports _n_-way transfers, HTTP-sourced data can be sent to any `DataSink` type and an
HTTP endpoint can receive data from any `DataSource` type. The extension is designed to stream content to limit memory
consumption under load.

### Use Cases

Typically, this extension is used to fetch or post data from/to a REST endpoint.

## Technical Details

### Dependencies

| Name                                 | Description           |
|:-------------------------------------|:----------------------|
| extensions:data-plane:data-plane-spi | SPI of the data plane |

### Configurations

The setting parameters of this extension are listed below:

| Parameter name                                      | Description                                                          | Mandatory | Default value |
|:----------------------------------------------------|:---------------------------------------------------------------------|:----------|:--------------|
| `edc.dataplane.http.sink.partition.size`  | Number of partitions for parallel message push in the `HttpDataSink` | false     | 5             |

## Design Principles

This extension provides implementations for `HttpDataSourceFactory`, `HttpDataSinkFactory`, `HttpDataSource` and `HttpDataSink`, which are triggered when
the `DataFlowRequest` type is `HttpData`.

Basically the role of these classes is to extract the parameters (query params, path, body, auth header...) required to hit the HTTP endpoint and then to perform the call.

The table below summarizes how each parameter is retrieved for the source/sink implementations.

| Parameter                 | `HttpDataSource`                                                                                             | `HttpDataSink`                                                    | Example                              |
|:--------------------------|:-------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------|:-------------------------------------|
| Base url                  | Source/destination `DataAddress`                                                                             | Same                                                              | http://example.com                   |
| Authorization header key  | Source/destination `DataAddress`                                                                             | Same                                                              | Api-Key                              |
| Authorization header code | Source/destination `DataAddress`, which can either contains the secret value, or the `Vault` secret name     | Same                                                              | 8e631012-f6de-11ec-b939-0242ac120002 |
| Headers                   | Source/destination `DataAddress`                                                                             | Same                                                              | foo:bar;hello:world                  |
| Path                      | `DataFlowRequest` properties if path proxy enabled by the source `DataAddress`                               | Destination `DataAddress`                                         | foo/bar                              |
| Query params              | `DataFlowRequest` properties if query param proxy enabled by the source `DataAddress`                        | Destination `DataAddress`                                         | hello=world&foo=bar                  |
| Method                    | `DataFlowRequest` properties if method proxy enabled by the source `DataAddress`, otherwise default to `GET` | Destination `DataAddress` if present, otherwise `POST` by default | GET, POST...                         |
| Content type              | `DataFlowRequest` properties if body proxy enabled by the source `DataAddress`                               | Destination `DataAddress`                                         | application/json                     |
| Body                      | `DataFlowRequest` properties if body proxy enabled by the source `DataAddress`                               | `Part` stream fetched by the `DataSource`                         | "hello world!"                       |
