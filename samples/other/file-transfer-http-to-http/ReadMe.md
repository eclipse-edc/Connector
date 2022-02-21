
# Transfer HTTP to HTTP

This demo configuration uses specific implementations of `DataReader` and `DataWriter` interfaces to do a file transfer using
pure HTTP.

The demo can be run with a single connector acting as both consumer and provider. Destination URL must be provided with artifact request.
Source URL can be specified via `edc.demo.http.source.url` property.
Destination URL can be specified via `edc.demo.http.destination.url` property.

## Steps

* Get offers
* Negotiate contract, 
  * Use one of the offers from the previous step's response 
    * **IMPORTANT:** fill provider and consumer attributes (e.g. "https://provider.com", "https://consumer.com")
  * Wait for the agreement confirmation 
* Request the artifact
* Wait for the transfer process to complete

Please use [postman collection](samples/other/file-transfer-http-to-http/postman/IDS Eclipse HTTP to HTTP.postman_collection.json).

## Build

    ./gradlew :samples:other:file-transfer-http-to-http:shadowJar

## Run

    java -Dedc.fs.config=samples/other/file-transfer-http-to-http/config.properties -jar samples/other/file-transfer-http-to-http/build/libs/file-transfer-http-to-http.jar