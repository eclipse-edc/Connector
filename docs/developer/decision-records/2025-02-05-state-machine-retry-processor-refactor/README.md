# State Machine Retry processor refactor

## Decision

We'll refactor the `RetryProcess` component that's currently used by the `*Manager` components to provide retry mechanism
for actions executed during state transitions.

## Rationale

The current `RetryProcess` design doesn't permit to have multiple operations concatenated. This missing feature has been
tricked with including subsequent actions into the first one `onSuccess` block, like ([ref.](https://github.com/eclipse-edc/Connector/blob/7552e8ef9fbc36e275093b946b08350818ea2c3c/core/control-plane/control-plane-transfer/src/main/java/org/eclipse/edc/connector/controlplane/transfer/process/TransferProcessManagerImpl.java)):

```java
@WithSpan
private boolean processTerminating(TransferProcess process) {
    if (process.getType() == CONSUMER && process.getState() < REQUESTED.code()) {
        transitionToTerminated(process);
        return true;
    }

    return entityRetryProcessFactory.doSyncProcess(process, () -> terminateDataFlow(process))
            .onSuccess((p, dataFlowResponse) -> sendTransferTerminationMessage(p))
            .onFailure((t, failure) -> {
                if (t.terminationWasRequestedByCounterParty()) {
                    transitionToTerminatingRequested(t, failure.getFailureDetail());
                } else {
                    transitionToTerminating(t, failure.getFailureDetail());
                }
            })
            .onFatalError((p, failure) -> transitionToTerminated(p, failure.getFailureDetail()))
            .onRetryExhausted((p, failure) -> transitionToTerminated(p, failure.getFailureDetail()))
            .execute("Terminate data flow");
}

private boolean sendTransferTerminationMessage(TransferProcess process) {
    var builder = TransferTerminationMessage.Builder.newInstance()
            .reason(process.getErrorDetail());

    var dispatch = process.terminationWasRequestedByCounterParty()
            ? entityRetryProcessFactory.doAsyncStatusResultProcess(process, doNothing())
            : dispatch(builder, process, policyArchive.findPolicyForContract(process.getContractId()), Object.class);

    return dispatch
            .onSuccess((t, content) -> {
                transitionToTerminated(t);
                if (t.getType() == PROVIDER) {
                    transitionToDeprovisioning(t);
                }
            })
            .onFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
            .onFatalError((n, failure) -> transitionToTerminated(n, failure.getFailureDetail()))
            .onRetryExhausted(this::transitionToTerminated)
            .execute("send transfer termination to " + process.getCounterPartyAddress());
}
```

In this case the `sendTransferTerminationMessage` is instantiating a new `RetryProcess` (in the `dispatch` method) with
a different set of handlers (that are pretty much duplicated from the ones on top).

This design doesn't permit the code to be expressed in a clear and fluent representation, in addition to requiring duplicated
error handler.

The current design has also additional flaws, as:
- `onFatalError` and `onRetryExhausted` handled in a different way, but in fact they should be the same, because a "retry
  exhausted" error can be considered a fatal error, an event that must stop retry
- a lot of logic is duplicated in the `RetryProcess` subclasses
- hard to understand abstract design

The current implementation will be deprecated, and a new implementation will be provided to be used in parallel at first,
until the deprecation expires and the old one can be deleted.

## Approach

### Concepts

- `Process`: a function that given an `Entity` and an input can execute a sync or async operation and returns the `Entity`
  and the output of the operation into an enclosing type
    - the enclosing type is `CompletableFuture`, this because it permits to handle sync and async operation interchangeably.
- `RetryProcessor` the component that will permit to set a chain of `Process`es that can be executed. Based on the result
  of the operation and the retry conditions it will call one of the handlers:
    - `onSuccess`: is called when all the processors execute successfully
    - `onFailure`: is called when there's a failure but the operation is meant to be retries, depending on how many retries
      the entity already went through and what is the configured `retryLimit`
    - `onFinalFailure`: is called when retries are expired or an unrecoverable error happens
      the processor returns true` if the at least one process gets executed or `false` if not (such case happens when a retry
      strategy is waiting to be satisfied and processing will happen again on the next state machine iteration)

Please note that all the `Process`es need to be idempotent, this will permit intermediate failures to be

### Design

#### ProcessContext
It is a simple `record` that wraps an entity instance and a content that will be used as input and output of the `Process`:
```java
public record ProcessContext<E extends StatefulEntity<E>, C>(E entity, C content) { }
```

#### Process
It's a component that given a `ProcessContext` in input, it returns a `CompletableFuture<ProcessContext>`, it takes care
of executing the operation, returning the content wrapped in a `CompletableFuture` and handling eventual exception in the
proper way, distinguishing standard errors with unrecoverable errors.

The interface looks like:
```java
public interface Process<E extends StatefulEntity<E>, I, O> {
    CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context);
}
```

Different `Process` implementation will be provided to be able to handle different operation return types:
- `result`: `StatusResult<C>`
- `future`: `CompletableFuture<C>`
- `futureResult`: `CompletableFuture<StatusResult<C>>`

#### RetryProcessor

The `RetryProcessor` will have a `processorChain` field of type `Function<Void, CompletableFuture<ProcessContext<E, C>>>`.
Let's break it down:
- the processor chain when executed will accept `null` and it will return a `CompletableFuture<ProcessContext<E, C>>`
- `ProcessContext` is just a `record` that contains the `Entity` (of type `E`) involved and the content (of type `C`).
- the `Function` has a `Void` input type because for the first process won't have an input.

To chain different processors the approach is to start with an "identity" process chain defined as:
```
v -> CompletableFuture.completedFuture(new ProcessContext<>(entity, null))
```

and then, for every processor added it gets overridden by another `processChain` that calls the previous and it concatenates
the processor:
```
public <C1> RetryProcessor<E, C1> doProcess(Process<E, C, C1> process) {
    return new RetryProcessor<>(entity, monitor, clock, configuration, c -> processChain.apply(c).thenCompose(process));
}
```

### Usage

That's how it will look from the outside (example taken from the `TransferProcessManager.startTransferFlow` method):
```java
        ...
        // instantiate the retry processor on a `TransferProcess Instance
        return entityRetryProcessFactory.retryProcessor(process)
                // add a first processor that executes synchronously and returns a `StatusResult`
                .doProcess(result("Start DataFlow", (t, c) -> dataFlowManager.start(process, policy)))
                // add a second processor that executes asynchronously and returns a `CompletableFuture<StatusResult>>`
                .doProcess(futureResult("Dispatch TransferRequestMessage to: " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> {
                            var messageBuilder = TransferStartMessage.Builder.newInstance().dataAddress(dataFlowResponse.getDataAddress());
                            return dispatch(messageBuilder, t, Object.class).<StatusResult<DataFlowResponse>>thenApply(result -> result.map(i -> dataFlowResponse));
                        })
                )
                // on success handler, it will get the output of the last processor in input
                .onSuccess((t, dataFlowResponse) -> transitionToStarted(t, dataFlowResponse.getDataPlaneId()))
                // on failure handler
                .onFailure((t, throwable) -> onFailure.accept(t))
                // on retry exhausted or unrecoverable error handler
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
```
