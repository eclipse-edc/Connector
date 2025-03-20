# Retry Processor on Dataflow Completion

## Decision

The retry mechanism will be applied in the `DataFlowManagerImpl` when processing the `Dataflow`'s *COMPLETED* and *FAILED* states.

## Rationale

After the data transfer, the `DataFlow` transitions to *COMPLETED* or to *FAILED* if the transfer succeeded or failed, respectively. In either case, the data plane notifies the control plane of the transfer outcome using the `TransferProcessApiClient`. If the notification succeeds, the `DataFlow` correctly transitions to *NOTIFIED*. However, if the notification fails, the `DataFlow` transitions to its current state, causing the state machine manager to pick it up again and retry the process. Since there is no limit to the number of retries, this process could potentially continue forever. The following code block shows how the *COMPLETED* and *FAILED* states are processed by `DataPlaneManagerImpl`.

```java
private boolean processCompleted(DataFlow dataFlow) {
    var response = transferProcessClient.completed(dataFlow.toRequest());
    if (response.succeeded()) {
        dataFlow.transitToNotified();
        update(dataFlow);
    } else {
        dataFlow.transitToCompleted(); // Will retry while the process fails
        update(dataFlow);
    }
    return true;
}

private boolean processFailed(DataFlow dataFlow) {
    var response = transferProcessClient.failed(dataFlow.toRequest(), dataFlow.getErrorDetail());
    if (response.succeeded()) {
        dataFlow.transitToNotified();
        update(dataFlow);
    } else {
        dataFlow.transitToFailed(dataFlow.getErrorDetail()); // Will retry while the process fails
        update(dataFlow);
    }
    return true;
}
```

Applying a `RetryProcessor` to both `processCompleted()` and `processFailed()` provides greater control over these processes, as it allows to define specific behavior after a configurable number of failed retry attempts.

## Approach

Both `TransferProcessApiClient` calls - `transferProcessClient.completed()` and `transferProcessClient.failed()` - will be wrapped in a `ResultRetryProcess`, allowing them to be executed by a `RetryProcessor`. Since `ResultRetryProcess` expects its executable function to return a `StatusResult`, but both `TransferProcessApiClient` calls return a `Result`, these methods will be updated accordingly.

In each case, the process outcomes will be handled by event handlers managing success, failures, and final failure. Both `processCompleted()` and `processFailed()` will define these handlers with the following behavior.

- On success, the `DataFlow` will transition to *NOTIFIED*, keeping current successful behavior
- On failure, the `DataFlow` will transition to it's current state, allowing the process to be retried
- On final failure, the `DataFlow` will transition to *TERMINATED*, so that it doesn't retry anymore
