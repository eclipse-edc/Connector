# Demo Asset Index

This sample shows how to interact with an in-memory asset index through a REST API.

Steps:
* Build and run the sample with this command (launched from the root path of the DataSpaceConnector):
```
./gradlew :samples:demo-asset-index:shadowJar && (cd samples/demo-asset-index && java -jar build/libs/demo-asset-index.jar)
```
* Add entries to the asset index:
```
curl -X POST -H 'Content-Type: application/json' -d '{"id":"anId", "path":"/aValidPath"}' http://localhost:8181/api/assets
```
* Retrieve the stored asset index:
```
curl http://localhost:8181/api/assets
```