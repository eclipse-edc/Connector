# Using the Command Queue

In many situations, especially when there are is parallel processing in multiple threads involved, we may run into situations that could potentially cause
inconsistencies. For the purposes of this document we'll use the `TransferProcessManager` ("TPM") as example, although the concept applies to other components
as well.

## Problem statement

The `TransferProcessManager` automatically updates `TransferProcess` ("TP") objects according to its state machine and depending on external influence, e.g.
provisioning. This is done on a separate thread. However, there are situations, where those TransferProcesses need to be modified from a different thread, for
example a watchdog, that retires timed-out or stale TPs. Simply fetching the TP from the store, updating it and storing it again may cause race conditions, and
it may cause the TP to be in an unexpected state.

Thus, the `TransferProcessStore` should never be accessed directly, instead every action that modifies a TP, that happens _outside_ of the TPM should be
enqueued in the `CommandQueue`. That queue is drained at a specific time in the state machine, thus all actions are executed in a serial manner, which is
necessary to again avoid race conditions.

## When to use it

Whenever access a shared resource like a TP is required, the respective piece of code be wrapped in a `CommandHandler` and enqueued in the `CommandQueue`. The
CommandQueue will then execute the code safely and without side effects.

## How to use it

First it's important to note that `Commands` are mere POJOs, their contract is that they are fully serializable, so they cannot have references to any
non-serializable objects.

Commands typically contain the data, that the `CommandHandler` needs to execute the code, for example the ID of a transfer process. `CommandHandler`s must be
registered at runtime start, which is typically done in a `ServiceExtension`:

```java
public class YourExtension {
    @Inject
    private CommandHandlerRegistry registry;

    @Override
    public void initialize() {
        var commandHandler = new HelloCommandHandler(); //handles HelloCommands commands
        registry.register(commandHandler);
    }
}
```

Whenever a `HelloCommand` command needs to be issued in client code, a reference to the `CommandQueue` is required. For safety reasons the `CommandQueue`
is not exposed directly, but only accessible through e.g. a `TransferProcessManager`:

```java
public class ClientClass {

    private TransferProcessManager tpm;

    public void sayHello() {
        var command = new HelloCommand();
        tpm.enqueueCommand(command);
    }
}
```

After that, the command will get executed eventually, but there is no guarantee with regard to latency, timeouts, ordering or resilience. The only certainty is
that the command will be executed once. If it fails, it gets re-enqueued, or discarded, if its retry cycles are exhausted.

Note that there is no direct back channel to the issuer of the command. While it would have been easy to add a `CompletableFuture` as return type, it would
violate the serializability principle.

## Best practices

- do not maintain any references to non-serializable objects in the `Command` object
- Commands should not have behaviour, only state
- Command handlers should not have state, only behaviour
- As command handlers must be stateless, they must determine their state, for example by executing a database `read()` directly in its `handle()` method.
- Command handlers must be defensive, for example they must check whether a transfer process can be modified or not.
- Command handler should not implement long-running operations, as they may hold up calling components such as the TPM.
- if command handlers throw an exception, the command may get retried, so proper cleanup must be done.
- wrap all `enqueue` operations in a `try-catch` block, because enqueuing may fail with an `IllegalStateException`, e.g. when the queue is full (bounded queue)
- do not expose the `CommandQueue` directly - clients should only be able to enqueue items.

## Further reading

A complete sample with additional explanation can be found in the [samples](/samples/04.2-modify-transferprocess/README.md).
