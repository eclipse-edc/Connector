# Event Framework Refactoring

## Decision

This decision record covers the refactor and improvements of the Event Framework in two main aspects:

- Simplify the definition and maintenance of event types.
- A new way of typed subscriptions based on event type.

The `Event` and the `EventPayload` will be merged together in the `Event` class representing the payload abstraction, while
a new concrete class `EventEnvelope` will be introduced for holding and dispatching the previously mentioned `Event`,
and it will contain all the event metadata like id, timestamp, etc..

Additionally, we will introduce/refactor methods in the `EventRouter` and `EventSubscriber` for subscribing to events based on their kind.

## Rationale

From an EDC contributor prospective adding a new `Event` today requires to implement at least two classes, the `Event`
and the `EventPayload` with their builders.
While the hierarchy on the `EventPayload` is useful to express and abstract the event payload, the abstraction on the
metadata `Event` can be simplified, considering that events metadata rarely changes in terms of properties.

## Approach

The `Event` and the `EventPayload` classes will be merged together in an abstract class called `Event`, that all EDC
events should implement. The `Event` will represent the abstraction over the event payload carried by a new concrete class called `EventEnvelope` which will be generic over the `Event`.

Here's how the envelope will look like:

```java
public class EventEnvelope<E extends Event> {

    private String id;

    private long at;
    private E payload;
    ...

    // Builder
}
```

When creating a new event in EDC the only class to extend is `Event` and all current EDC events needs to be
migrated to the new shape.

For example the `AssetCreated` could be:

```java
public class AssetCreated extends Event {

    private String id;

    // Builder
```

The signature of the `EventRouter#publish` also need to change for taking in input the `EventEnvelope`:

```java
public interface EventRouter {
    void publish(EventEnvelope event);
}
```

Here's an example on how publishing would look like on `AssetListenerImpl#created`:

```java
public void created(Asset asset){
    var event=EventEnvelope.Builder.newInstance()
        .id("event-id")
        .at(Clock.systemUTC().millis())
        .payload(AssetCreatedNew.Builder.newInstance().id("id").build())
        .build();
    eventRouter.publish(event);
}
```

For introducing the typed subscribers the interfaces for registering and handling events also needs to be changed.

The `EventSubscriber` will change as follows:

```java
public interface EventSubscriber<T> {
    void on(EventEnvelope<T> event);
}
```

And the `EventRouter#register/{async}` method will take in input:

- the type of the event that the subscriber is interested in
- the subscriber itself

```java
public interface EventRouter {
    <E extends Event> void register(Class<E> eventKind, EventSubscriber<E> subscriber);
}
```

The default router `EventRouterImpl` and other implementors should take into account the events hierarchy
when registering subscribers and dispatching events, considering that the base event class will be `Event`.

For example if a subscriber is interested in all events, an `EventSubscriber` dealing with the `Event.class` type can be used.

The polymorphic subscribers should work also with custom hierarchy of events. If we consider the current `Asset` events
we could model them in the following way:

```java
public class AssetEvent extends Event {
}

public class AssetCreatedEvent extends AssetEvent {
}

public class AssetDeletedEvent extends AssetEvent {
}
```

Then a subscriber can use an `EventSubscriber` for a specific `Asset` event (e.g. `AssetDeleted` or `AssetCreated`) or
just subscribe to all `Asset` events by using the base class `AssetEvent`.
