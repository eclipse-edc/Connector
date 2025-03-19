# Retry Processor on Dataflow Completion

## Decision

The retry mechanism will be applied in the `DataFlowManagerImpl` when processing the `Dataflow`'s *COMPLETED* and *FAILED* states.

## Rationale

The data plane notifies the control plane of the success or failure of the data transfer. As of right now, if this notification fails, the `DataFlow` transitions to its current state, being picked up again by the state machine manager and retrying the process. As there is no limit to the amount of retries, this process could possibly continue forever.

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

By adding a `RetryProcessor`, we may define handlers for when the process is successful, when it fails but can be retried, and when it reaches final failure.

## Approach

Both `TransferProcessApiClient` calls - `transferProcessClient.completed()` and `transferProcessClient.failed()` - will be wrapped in a `Process`, so that a `RetryProcessor` may execute it.

- On success, the `DataFlow` will transition to *NOTIFIED*, keeping current successful behavior
- On failure, the `DataFlow` will transition to it's current state, allowing the process to be retried
- On final failure, the `DataFlow` will transition to *TERMINATED*, so that it doesn't retry anymore
