# Retry Processor on Dataflow Completion

## Decision

The retry mechanism will be applied in the `DataFlowManagerImpl` when processing the `Dataflow`'s *COMPLETED* and *FAILED* states.

## Rationale

After the data transfer, the `DataFlow` transitions to either *COMPLETED* or *FAILED* depending on whether the transfer succeeded or failed. This transition triggers the `processCompleted()` or `processFailed()` methods from `DataFlowManagerImpl` respectively. In either case, the data plane notifies the control plane of the transfer outcome using the `TransferProcessApiClient`, invoking either `transferProcessClient.completed()` or `transferProcessClient.failed()`. If the notification succeeds, the `DataFlow` correctly transitions to *NOTIFIED*. However, if the notification fails, the `DataFlow` transitions to its current state, causing the state machine manager to pick it up again and retry the process. Since there is no limit to the number of retries, this process could potentially continue forever. Applying a `RetryProcessor` to both `processCompleted()` and `processFailed()` provides greater control over these processes, as it allows to define specific behavior after a configurable number of failed retry attempts.

## Approach

Both `TransferProcessApiClient` calls - `transferProcessClient.completed()` and `transferProcessClient.failed()` - will be wrapped in a `ResultRetryProcess`, allowing them to be executed by a `RetryProcessor`. Since `ResultRetryProcess` expects its executable function to return a `StatusResult`, but both `TransferProcessApiClient` calls return a `Result`, these methods will be updated accordingly.

In each case, the process outcomes will be handled by event handlers managing success, failures, and final failure. Both `processCompleted()` and `processFailed()` will define these handlers with the following behavior.

- On success, the `DataFlow` will transition to *NOTIFIED*, keeping current successful behavior
- On failure, the `DataFlow` will transition to it's current state, allowing the process to be retried
- On final failure, the `DataFlow` will transition to *TERMINATED*, so that it doesn't retry anymore

The following code block represents how the `processCompleted()` and `processFailed()` methods will look like:

```java
private boolean processCompleted(DataFlow dataFlow) {
    return entityRetryProcessFactory.retryProcessor(dataFlow)
            .doProcess(Process.result("Complete data flow", (d, v) -> transferProcessClient.completed(dataFlow.toRequest())))
            .onSuccess((d, v) -> {
                dataFlow.transitToNotified();
                update(dataFlow);
            })
            .onFailure((d, t) -> {
                dataFlow.transitToCompleted();
                update(dataFlow);
            })
            .onFinalFailure((d, t) -> {
                dataFlow.transitToTerminated(t.getMessage());
                update(dataFlow);
            })
            .execute();
}

private boolean processFailed(DataFlow dataFlow) {
    return entityRetryProcessFactory.retryProcessor(dataFlow)
            .doProcess(Process.result("Fail data flow", (d, v) -> transferProcessClient.failed(dataFlow.toRequest(), dataFlow.getErrorDetail())))
            .onSuccess((d, v) -> {
                dataFlow.transitToNotified();
                update(dataFlow);
            })
            .onFailure((d, t) -> {
                dataFlow.transitToFailed(dataFlow.getErrorDetail());
                update(dataFlow);
            })
            .onFinalFailure((d, t) -> {
                dataFlow.transitToTerminated(t.getMessage());
                update(dataFlow);
            })
            .execute();
}
```
