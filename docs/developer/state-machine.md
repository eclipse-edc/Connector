# State machine

EDC provides a lightweight framework to facilitate the development of persistent state machines.

The framework currently manages a single execution thread.

## Collaborators

- The class which defines state machine instances. The `StatefulEntity` base class can be used to derive state machine 
  entity classes. For example, a `ContractNegotiation` is a `StatefulEntity` for an EDC contract negotiation.
- The `StateMachineManager` which manages an execution thread that periodically gives a chance to state machines to 
  progress their state.
- The state-machine specific Manager which instantiates the `StateMachineManager` and defines processors for each state
  for a given state machine class. For example, `ConsumerContractNegotiationManagerImpl` manages `ContractNegotiation`s
  in which the connector is a consumer.
- The `ServiceExtension` which manages the Manager's lifecycle.
- The Store which manages `StatefulEntity` persistence. `InMemoryStatefulEntityStore` provides a utility class to back
  in-memory implementations for testing.

## State-machine specific Manager

The Manager manages the `StateMachineManager`'s lifecycle and defines the state machine's behavior, while the `StatefulEntity`
is only concerned with the state machine's data.

Here's a prototypical Manager implementation:

```java
public class EntityManager {
    
    public void start() { // Called from ServiceExtension start() method
        stateMachineManager = StateMachineManager.Builder.newInstance("state-machine-name", monitor, executorInstrumentation, waitStrategy)
                // Processors for non-terminal states
                .processor(processEntitiesInState(STATE1, this::processState1))
                .processor(processEntitiesInState(STATE2, this::processState2))
                .processor(processEntitiesInState(STATE3, this::processState3))
                .build();

        stateMachineManager.start();
    }

    public void stop() { // Called from ServiceExtension shutdown() method
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    private Processor processEntitiesInState(State state, Function<StatefulEntityImpl, Boolean> function) {
        var filter = new Criterion[] { hasState(state.code()), isNotPending() };
        return ProcessorImpl.Builder.newInstance(() -> transferProcessStore.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending) // a guard can be added to, e.g. put in pending certain entities based on the `pendingGuard` predicate
                .build();
    }

    // Processor functions should return true only if the state machine has been updated
    private boolean processState1(StatefulEntityImpl sm) {
        if (conditionsForTransitionFromState1ToState2Apply(sm)) {
            sm.transitionState2();
            store.save(sm);
            return true;
        }
        return false;
    }

}
```

### Guards
On a state machine `Processor` a `Guard` can be specified. that's a way to have a custom flow based on a predicate that can
be extended, for example, to enable "external interactions" in the state machine, as user interactions. A `Guard` predicate
can be set on the processor with a specific process to be executed. This way when the predicate matches the entity can be 
set to pending, making it "invisible" for the state machine, but still accessible and modifiable by users or external systems.

`Guard` example:
```java
class EntityPendingGuard implements PendingGuard<Entity> {
    
    // custom collaborators as other services
    
    boolean test(Entity entity) {
        // additional logic
        return entity.getState() = SPECIFIC_STATE.code() && otherCondition; // if true, the entity will be set as pending
    }
    
}
```

## State-machine store

The Store which manages `StatefulEntity` persistence must persist entities in a storage system. In-memory implementations
are provided for testing.

Stores using persistent implementations must manage leases to support EDC clustered deployment. This ensures an entity is
processed by only one EDC instance at a time (assuming processing is quicker than lease expiration).

```java
class EntityStore {
    public void save(StatefulEntityImpl instance) {
      // persist instance
      // release lease
    }
    
    List<T> nextNotLeased(int max, Criterion... criteria); {
      // retrieve and lease at most limit instances that satisfy criteria
    }
}
```
