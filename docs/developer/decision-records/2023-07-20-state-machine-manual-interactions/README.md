# State Machine manual interactions

## Decision

The EDC will provide a way to permit manual interactions over automatic ones in the state machine processes.

## Rationale

In the EDC domain, the state machine is currently a completely automatic engine, every state transition is pre-defined
and they are following completely automatic logic.

With the introduction of the so-called "counter offer" feature, there's the need to permit to avoid the state machine to
pick up certain states and having the user interacting with it through commands in the Management API.

This will permitted in the most generic way possible, giving to every state machine the possibility to "interrupt" the
automatic flow and let the user act on it.

## Approach

This implementation is based on two pillars:
- adding a generic hook functionality to the state machine
- add a flag that permits to recognize entities that are waiting for manual interactions

The first one will be implemented in the `StateProcessorImpl`, that's the component that executes the state machine logic.
In the new implementation, there will be the possibility to add a `Hook`, that's a tuple of a `Predicate` and a `Function`.
The new `StateProcessorImpl` flow will be:
```java
entities.get().stream()
    .map(entity -> {
        if (hook.predicate().test(entity)) {
            return hook.process().apply(entity);
        } else {
            return process.apply(entity);
        }
    })
    .filter(isEqual(true))
    .count()
```

so the `Hook` will take over in the case its predicate matches the entity.

This way it will be possible to control the flow, and every `*Manager` (`ContractNegotiation`, `TransferProcess`, ...) 
can then register their own `ManualInteractionPredicate` on the state machine configuration.
In particular, the default hook function implementation will set the "waiting for manual interaction" flag on the entity,
like: 
```
Function<Entity> hookFunction = entity -> {
    entity.setWaitingForManualInteraction(true);
    update(entity);
    return true;
```

The flag will be then used as an additional filter passed to the `store.nextNotLeased` method used by the state machine
to filter out such entities. This way they will just stay still in the database waiting for a manual interaction.

The hook predicate will be completely extensible and will permit the implementors to decide if a specific entity state needs
manual interaction based on the input, like:
```java
class EntityManualInteractionPredicate implements ManualInteractionPredicate<Entity> {
    
    // custom collaborators as other services
    
    boolean test(Entity entity) {
        // custom condition
        return entity.getState() = SPECIFIC_STATE.code() && otherCondition;
    }
    
}
```
