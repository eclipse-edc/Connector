# Data Plane HTTP extension

This extension provides support for sending data sourced from an HTTP endpoint and posting data to an HTTP endpoint. By
nature of the DPF design, which supports _n_-way transfers, HTTP-sourced data can be sent to any `DataSink` type and an
HTTP endpoint can receive data from any `DataSource` type. The extension is designed to stream content to limit memory
consumption under load.

Note that Azure Object Storage or S3 extensions should be preferred to the current extensions when performing large data
transfers as support more scalable parallelization.

# Configuration

## Configuration properties

* edc.dataplane.http.sink.partition.size - Sets the number o parallel partions for the sink (default set to 5).

## Data properties

see [HttpDataAddress.java](../../../spi/core-spi/src/main/java/org/eclipse/dataspaceconnector/spi/types/domain/HttpDataAddress.java)

* type - The HTTP transfer type is "HttpData".
* endpoint - The http endpoint.
* name - The name associated with the HTTP data, typically a filename (optional).
* authKey - The authentication key property name (optional).
* authCode - The authentication code property name (optional).
* secretName - The name of the vault secret that is containing the authorization code (optional).
* proxyBody - If set to true the body of the actual request will be used to retrieve data from this address.
* proxyPath - If set to true the path of the actual request will be used to retrieve data from this address.
* proxyQueryParams - If set to true the query params of the actual request will be used to retrieve data from this address.
* proxyMethod - If set to true the http method of the actual request will be used to retrieve data from this address.
* header:* - The additional headers to use as json string e.g. ```"header:Content-Type" : "application/octet-stream","header:x-ms-blob-type": "BlockBlob"```.