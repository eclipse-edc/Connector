# Modify a TransferProcess

In the last samples (`04.0` and `04.1`) we saw how data can be transferred easily, what a `TransferProcess` is and how
to react to it easily through the listener. This sample will show how `TransferProcess` objects can be modified
externally in a thread-safe and consistent way.



## Problem statement

The `TransferProcessManager` (TPM), which is the central state machine handling transfer processes, follows this basic
operational pattern:

1. take transfer process (TP) out of `TransferProcessStore` (TPS)
2. take appropriate action, e.g. provision or deprovision ressources
3. update state of TP
4. put back into TPS

All those steps happen in a non-atomic way, so when a TP currently processed by the TPM is modified on another thread,
there is a strong possibility that that change will get overwritten or worse, may cause the state machine to be in an
illegal state.

A common pattern would be some sort of watchdog, where TPs that have not advanced their state in a given amount of time
are automatically cancelled or errored out. The following code snippet shows a typical TPM state transition:

```java
// get out of store
 var tpList = store.nextForState(IN_PROGRESS.code(),batchSize);
// take appropriate action, e.g. check if complete
var statusChecker=...;
foreach(var tp:tpList){
    if(statusChecker.isComplete()){
        //update state
        tp.transitionComplete();
        // put back into TPS
        store.update(tp);
    }
}
```
and then consider a watchdog that runs on another thread and fires every X minutes
```java

private void watchDog(){
  var longRunningTpList = store.nextForState(IN_PROGRESS.code(), 5);
  // filter list based on last state update
  var longRunningTpList = /*filter expression*/;
  for(var tp : longRunningTpList){
      tp.transitionError("timeout");
      store.update(tp);
  }
}
```

Now the problem becomes apparent when the `watchDog()` fires exactly here:
```java
//...
    if(statusChecker.isComplete()){
        
        // |<-- watchDog() fires here!
            
        //update state
        tp.transitionComplete();
        // ...
    }
```

then the TP would first go to the `ERROR` state, but then immediately to the `COMPLETED` state, because the TPM and the watchdog
have different object references to the same TP. We essentially have a race condition at our hands, resulting in the TP never 
"erroring out".

## The `CommandQueue`
The way to go around this is to create a `Command` and a respective `CommandHandler`, register both with the transfer state machine and 
when the time comes to send a TP to `ERROR`, simply submit the `Command` object.

Commands are the "what", handlers are the "how", so we separate the desired state from the actual action to be taken, they always exist
in tandem. Command handlers have to be registered with the `CommandHandlerRegistry`:
```java
// in YourExtension.java

//...
@Inject
private CommandHandlerRegistry registry;

public void initialize(ServiceExtensionContext context){
    registry.register(new CheckTimeoutCommandHandler(/*left out for clarity*/);
}
```

## How to use it 
New commands can be inserted into the queue through the `TransferProcessManager`. Although this might not be obvious at first, because
one might expect to insert commands directly into the queue, there is actually good reasoning for this.
Exposing the `CommandQueue` would also expose its entire API including `peek()` and `dequeue()`, which would be a dangerous thing.

Also, most clients will already have a reference to the `TransferProcessManager`, so little change needs to be done. Instead simply
do:
```java
tpm.enqueueCommand(new CheckTransferProcessTimeoutCommand(3, TransferProcessStates.IN_PROGRESS, Duration.ofSeconds(10)));
```

that will eventually get processed by the `TransferProcessManager`, resulting in log output similar to this: 

```bash
INFO 2022-01-14T12:45:38.176484 Running watchdog - submit command
INFO 2022-01-14T12:45:38.176795 will retire TP with id [tp-sample-04.2] due to timeout
DEBUG 2022-01-14T12:45:38.177363 Successfully processed command [class org.eclipse.dataspaceconnector.samples.sample042.CheckTransferProcessTimeoutCommand]
```

_Note: The command queue is not accessible through the `ServiceExtensionContext`, precisely for the aforementioned reason._

## About this sample
Please note that this sample does _not actually transfer anything_, it merely shows how to employ the `Command`/`CommandHandler`
infrastructure to modify a transfer process outside of the main state machine.

Modules:
- `simulator`: used to insert a dummy transfer process, that never completes to simulate the use of a watchdog
- `watchdog`: spins up a periodic task that sends a command to check for timed-out TPs and sets them to `ERROR`
- `consumer`: the build configuration

In order to run the sample, enter the following commands in a shell:

```bash
./gradlew samples:04.2-modify-transferprocess:consumer:build
java -Dedc.fs.config=samples/04.2-modify-transferprocess/consumer/config.properties -jar samples/04.2-modify-transferprocess/consumer/build/libs/consumer.jar
```
