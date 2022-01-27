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

Build, run the consumer, provider and Jaeger:

```bash
./gradlew samples:04.3-open-telemetry:consumer:build samples:04-file-transfer:provider:build
docker-compose -f samples/04.3-open-telemetry/docker-compose.yaml up
```

```bash
NEGOTIATION_ID=$(curl -X POST -H "Content-Type: application/json" -d @samples/04-file-transfer/contractoffer.json "http://localhost:9191/api/negotiation?connectorAddress=http://provider:8181/api/ids/multipart")
curl -X GET -H 'X-Api-Key: password' "http://localhost:9191/api/control/negotiation/${NEGOTIATION_ID}"
```

