# Demo Contract Framework

This framework provides an implementation of the ContractOfferFramework interface.

A simple REST API is offered to add assets to an in memory catalog.
The implemented PublicContractOfferFramework creates free use contract offers for all assets.

## Run
Build and execute:
```
./gradlew :samples:demo-contract-framework:shadowJar && (cd samples/demo-contract-framework && java -jar build/libs/connector.jar)
```

Add an asset:
```
curl -X POST -H 'Content-Type: application/json' -d '{"id":"anIda", "path":"/aValidPath"}' http://localhost:8181/api/assets
```

Query the contract offers:
```
curl http://localhost:8181/api/offers 
```
