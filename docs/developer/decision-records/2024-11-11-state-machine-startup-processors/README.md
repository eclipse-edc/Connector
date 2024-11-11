# State machine startup processors

## Decision

We will add the possibility to register one-time, at-startup processors to the `StateMachine` component.

## Rationale

A state machine could need some bootstrap work to getting ready at startup, as restarting stopped jobs.

## Approach

On the state machine builder there will be a new method that will permit to register a startup processor:
```java
Builder startupProcessor(Processor startupProcessor) {
    loop.startupProcessors.add(startupProcessor);
    return this;
}
```

The startup logic will be executed before the first iteration loop:
```java
for (var startupProcessor : startupProcessors) {
    try {
        startupProcessor.process();
    } catch (Throwable e) {
        monitor.severe(format("StateMachineManager [%s] startup error caught", name), e);
    }
}
```

At the end of the startup phase, the state machine will engage the main loop.
