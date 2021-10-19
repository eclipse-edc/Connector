# Transfer Functions SPI

This extension contains the SPI for the Transfer Functions feature. 

Transfer functions are endpoints the connector uses to initiate and control data transfers. Currently, HTTP-based functions are supported, which will enable connector operators to 
implement custom data transfer procedures on their technology infrastructure of choice via a simple endpoint interface.

# Status

In development. 
 
# HTTP Functions
   
## The Transfer Endpoint   

HTTP functions are implemented by configuring an HTTP(S) endpoint to accept a POST with `DataRequest` as the message type. The function is responsible for initiating data transfer 
based on the contained information. Return values will be interpreted as follows:

- `HTTP 200` code is returned to indicate successful initiation
- `HTTP 500-504` to indicate a retryable error
- Other values will be interpreted as a fatal error

## The Status Endpoint 
 
An HTTP(s) status endpoint must be configured that accepts a GET and returns a JSON-encoded boolean value. If true, the data transfer is complete; otherwise it is ongoing. 

## Using Interceptors
              
OKHttp interceptors can be added to mediate requests to a transfer function endpoint. For example, interceptors may decorate requests with an authentication header. 
See `TransferFunctionInterceptorRegistry`. 
