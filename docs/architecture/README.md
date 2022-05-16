# Architecture

- [Key Principles](architecture-principles.md)

## Configuration

Each EDC extension may use its own configuration settings and should explain them in their corresponding README.md.

For a more detailed explanation of the configuration itself please see [configuration.md](configuration/README.md).

## Data Transfer

### Contract

Before each data transfer a contract must be offered from the provider. A consumer must negotiate an offer successfully,
before its able to request data.

These two processes (offering & negotation) are documented in the [contracts.md](usage-control/contracts.md)

### TransferProcessListener

A consumer extension may register a listener to execute custom logic after a transfer changes state, for example, to notify an external application on the consumer side after data has been produced (i.e. the transfer moves to the completed state).

```java
transferProcessObservable = context.getService(TransferProcessObservable.class);
transferProcessObservable.registerListener(myTransferProcessListener);
```

A sample is available at [04.1-file-transfer-listener](/samples/04.1-file-transfer-listener).
