# State machine

EDC provides a lightweight framework to facilitate the development of persistent state machines.

The framework currently manage a single execution thread.

## Collaborators

- The class which defines state machine instances. The `StateMachine` base class can be used to derive state machine classes. For example, a `ContractNegotiation` is a `StateMachine` for an EDC contract negotiation.
- The `StateMachineManager` which manages an execution thread that periodically gives a chance to state machines to progress their state.
- The state-machine specific Manager which instantiates the `StateMachineManager` and defines processors for each state for a given state machine class. For example, `ConsumerContractNegotiationManagerImpl` manages `ContractNegotiation`s in which the connector is a consumer.
- The `ServiceExtension` which manages the Manager's lifecycle.
- The Store which manage `StateMachine` persistence. `InMemoryStateMachineStore` provides a utility class to back in-memory implementations for testing.

## State-machine specific Manager

The Manager manages the `StateMachineManager`'s lifecycle and defines the state machine's behavior, while the `StateMachine` is only concerned with the state machine's data.

Here's a prototypical Manager implementation:

```java
public void start() { // Called from ServiceExtension start() method
  stateMachine = StateMachine.Builder.newInstance("state-machine-name", ...)
    // Processors for non-terminal states
    .processor(processEntitiesInState(STATE1, this::processState1))
    .processor(processEntitiesInState(STATE2, this::processState2))
    .processor(processEntitiesInState(STATE3, this::processState3))
    .processor(onCommands(this::processCommand))
    .build();
  stateMachineManager.start();
}

public void stop() { // Called from ServiceExtension shutdown() method
  stateMachineManager.stop();
}

private StateProcessorImpl<StateMachineImpl> processEntitiesInState(State state, Function<StateMachineImpl, Boolean> function) {
  return new StateProcessorImpl<>(() -> store.nextForState(state, batchSize), function);
}

// Processor functions should return true only if the state machine has been updated
private boolean processState1(StateMachineImpl sm) {
    if (conditionsForTransitionFromState1ToState2Apply(sm)) {
      sm.transitionState2();
      store.save(sm);
      return true;
    }
   return false;
}
...
```

## State-machine store

The Store which manage `StateMachine` persistence must persist entities in a storage system. In-memory implementations are provided for testing.

Stores using persistent implementations must manage leases to support EDC clustered deployment. This ensures an entity is processed by only one EDC instance at a time (assuming processing is quicker than lease expiration).

```java
public void save(StateMachineImpl instance) {
  // persist instance
  // release lease
}

public Collection<StateMachineImpl> nextForState(State state, int limit) {
  // retrieve and lease at most limit instances in state
}
```
