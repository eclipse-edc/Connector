# Implement a simple transfer listener

In this sample, we build upon the [file transfer sample](../04-file-transfer) to add functionality to react to transfer completion on the consumer connector side.

We will use the provider from the [file transfer sample](../04-file-transfer), and the consumer built on the consumer from that sample, with a transfer process listener added.

Also, in order to keep things organized, the code in this example has been separated into several Java modules:

- `consumer`: this is where the extension definition and dependencies reside for the consumer connector
- `listener`: contains the `TransferProcessListener` implementation

The consumer also uses the `api` module (REST API) from the [file transfer sample](../04-file-transfer).

## Create the listener

A TransferProcessListener may define methods that are invoked after a transfer changes state, for example, to notify an external application on the consumer side after data has been produced (i.e. the transfer moves to the completed state).

```java
// in TransferListenerExtension.java
    @Override
    public void initialize(ServiceExtensionContext context) {
        // ...
        var transferProcessObservable = context.getService(TransferProcessObservable.class);
        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
    }
```

```java
public class MarkerFileCreator implements TransferProcessListener {

    /**
     * Callback invoked by the EDC framework when a transfer has completed.
     *
     * @param process
     */
    @Override
    public void completed(final TransferProcess process) {
        // ...
    }
}
```

## Perform a file transfer

Let's rebuild and run them both:

```bash
./gradlew samples:04.1-file-transfer-listener:consumer:build
java -Dedc.fs.config=samples/04.1-file-transfer-listener/consumer/config.properties -jar samples/04.1-file-transfer-listener/consumer/build/libs/consumer.jar
# in another terminal window:
./gradlew samples:04.0-file-transfer:provider:build
java -Dedc.fs.config=samples/04.0-file-transfer/provider/config.properties -jar samples/04.0-file-transfer/provider/build/libs/provider.jar
````

Assuming you didn't change the config files, the consumer will listen on port `9191` and the provider will listen on port `8181`.
Open another terminal window (or any REST client of your choice) and execute the following REST requests like in the previous sample:

```bash
curl -X POST -H "Content-Type: application/json" -d @samples/04.0-file-transfer/client/src/main/resources/contractoffer.json "http://localhost:9191/api/negotiation?connectorAddress=http://localhost:8181/api/v1/ids/data"
curl -X GET -H 'X-Api-Key: password' "http://localhost:9191/api/control/negotiation/{negotiation ID}/state"
curl -X POST "http://localhost:9191/api/file/test-document?connectorAddress=http://localhost:8181/api/v1/ids/data/&destination=/path/on/yourmachine&contractId={agreement ID}"
```

> **Replace `{negotiation ID}` in the second request with the UUID received as the response to the first request!**
>
> **Copy the contract agreement's ID from the second response, substitute it for `{agreement ID}` in the last request and adjust the `destination` to match your local dev machine!**

- the last path item, `test-document`, matches the ID of the `Asset` that we created earlier in
  `FileTransferExtension.java`, thus referencing the _data source_
- the first query parameter (`connectorAddress`) is the address of the provider connector
- the last query parameter (`destination`) indicates the desired _destination_ directory on you local machine.
- `curl` will return the ID of the transfer process on the consumer connector.

The consumer should spew out logs similar to:

```bash
INFO 2021-09-07T17:24:42.128363 Received request for file test-document against provider http://localhost:8181/
DEBUG 2021-09-07T17:24:42.592422 Request approved and acknowledged for process: 2b0a9ab8-78db-4753-8b95-77678cdd9fc8
DEBUG 2021-09-07T17:24:47.425729 Process 2b0a9ab8-78db-4753-8b95-77678cdd9fc8 is now IN_PROGRESS
DEBUG 2021-09-07T17:24:47.426115 Process 2b0a9ab8-78db-4753-8b95-77678cdd9fc8 is now COMPLETED
DEBUG 2021-09-07T17:24:47.426531 Transfer listener successfully wrote file /path/on/yourmachine/marker.txt

```

then check `/path/on/yourmachine`, which should now contain a file named `marker.txt` in addition to the file named `test-document.txt`.
