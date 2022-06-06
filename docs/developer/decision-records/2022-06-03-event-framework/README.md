# Event framework

## Decision

This decision record covers concepts and semantics about asynchronous communication in the EDC.
This is needed to define what's an `Event` and how this concept should be introduced into EDC.

## Rationale

There are two asynchronous communication levels in EDC:

#### Protocol level
Every call from a connector to another regards the communication protocol, currently the only protocol implemented in EDC is IDS.
A protocol call could have an asynchronous response, and that's called **callback**.

#### Instance level
The EDC should have the possibility to communicate with third party systems (known as subscribers) notifying them about what's happening in the connector. This communication needs to be made through **events**.
An event is a representation of something that happened in the past, and has very specific characteristics, like:
- it's named with a short sentence in the past tense (e.g. "contract negotiation confirmed")
- it's serializable
- it contains the issue timestamp
- it describes which layer emitted the event: domain, system, security, and so on...

#### Main differences between Events and Callbacks
- Callbacks are always correlated with a request, events are not
- Callbacks are unicast, events are multicast
- Callbacks are at the protocol level, events can be at a different level
- Callbacks are critical for the correct operation of a connector, where events are optional

## Approach

An event is generated after a specific state change happens.
In the EDC will be handled through the `Observable` pattern:

For every entity type, or for other layers a dedicated `Observable` implementation with a dedicated `Listener` should exist.
The `Listener` interface should have a method for every change that could happen on that entity. For example:
```java
public class TransferProcessEventObservableImpl implements Observable<TransferProcessEventListener> {
    ...
}

public interface TransferProcessEventListener {

    void created(TransferProcess process);

    void provisioned(TransferProcess process);

    ...

}
```

`Event` will be the superclass that represents all the events, and every event class will extend it.
```java
public abstract class Event {
    private final long createdAt;

    ...
}
```

Event example implementation:
```java
public class TransferProcessCreatedEvent extends Event {
    private String id;
    ...
}
```

The `Listener` would have a single implementation that will take care of generate the event class and pass it to the `EventRouter` component.

```java

public class TransferProcessEventListenerImpl implements TransferProcessEventListener {

    private final EventRouter eventRouter;

    public void created(TransferProcess process) {
        var event = TransferProcessCreatedEvent.Builder().newInstance()
            .id(process.getId())
            ...
            .build();

        eventRouter.publish(event);
    }

    ...

```

The `EventRouter` implementation will be a component that will permit the registration of `EventSubscriber`s and that will dispatch every event to every one of them, asynchronously:

```java
public interface EventSubscriber {
    void on(Event event);
}

public class EventRouterImpl implements EventRouter {

    private List<EventSubscriber> subscribers;

    public void publish(Event event) {
        subscribers.forEach(subscriber -> {
            CompletableFuture.runAsync(() -> subscriber.on(event));
        })
    }
}
```

At this point, at the extension level, a `EventListener` can be registered to the `EventRouter` and it will be able to get all the EDC events, filter them, publish them to some external event infrastructure.

Note that this implementation does not respect the "at-least-one" rule, as something can happen between event creation and its dispatch. 
To achieve such a behavior events should be persisted. With this implementation we would have events that will be delivered "at most once", they
should not be used to trigger important business logic but only to give insights on what's happening in the EDC.
