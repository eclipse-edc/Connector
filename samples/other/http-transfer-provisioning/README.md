# Perform a contract negotiation

**Note: this sample is based on [samples/other/file-transfer-provisioning](../../../samples/other/file-transfer-provisioning) sample**


## Forth test

For the forth test we will upload a http file to a cloud location:
* Using dataplane
* Create source and destination as httpdata (default dataplane http implementation)  - see [extensions/data-plane/data-plane-http/src/main/java/org/eclipse/dataspaceconnector/dataplane/http/pipeline/HttpDataSinkFactory.java](extensions/data-plane/data-plane-http/src/main/java/org/eclipse/dataspaceconnector/dataplane/http/pipeline/HttpDataSinkFactory.java)
* Use sink/destination as httpdata on provider side (the provider will push the data to the destination)
* Provision the destination url on consumer side

For this test we've prepared two new json sample files:
* httpcontractoffer.json
* httpfiletransfer.json

The order of curl calls are the same:
```bash
curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/http-transfer-provisioning/httpcontractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"

curl -X GET -H "Content-Type: application/json" -H "X-Api-Key: password"  "http://localhost:9192/api/v1/data/contractnegotiations"

curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/other/http-transfer-provisioning/httpfiletransfer.json "http://localhost:9192/api/v1/data/transferprocess"
```