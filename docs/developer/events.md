# Events

EDC provides an eventing system that permits to developers to write extensions that could react to events that are 
emitted from the core of the EDC and also emit custom events.

## Subscribe to events
The entry point for event listening is the `EventRouter` component, on which an `EventSubscriber` can be registered.

Actually, there are two ways to register an `EventSubscriber`:
- **async**: every event will be sent to the subscriber in an asynchronous way. Features:
  - fast, as the main thread won't be blocked during dispatchment. 
  - not-reliable, as an eventual subscriber dispatch failure won't get handled.
  - to be used for notifications and for send-and-forget event dispatchment.
- **sync**: every event will be sent to the subscriber in a synchronous way. Features:
  - slow, as the subscriber will block the main thread until the event is dispatched
  - reliable, an eventual exception will be thrown to the caller, and it could make a transactional context fail
  - to be used for event persistence and to satisfy the "at-least-one" rule.

The `EventSubscriber` is typed over the event kind (Class), and it will be invoked only if the type of the event matches 
the published one (instanceOf). The base class for all events is `Event`.
 
Extension example:
```java
public class ExampleEventSubscriptionExtension implements ServiceExtension {
    @Inject
    private EventRouter eventRouter;

    @Override
    public void initialize(ServiceExtensionContext context) {
        eventRouter.register(Event.class, new ExampleEventSubscriber()); // asynchronous dispatch
        eventRouter.registerSync(Event.class, new ExampleEventSubscriber()); // synchronous dispatch
    }
}
```

Then the `EventSubscriber` subscription will receive all the events emitted from the EDC and react to them:

```java
public class ExampleEventSubscriber implements EventSubscriber<Event> {
    
    public void on(EventEnvelope<Event> event) {
        // react to event    
    }
    
}
```

The `EventEnvelope` is used as a container for the `Event` itself. It will also have additional fields like

- id: unique identifier of the event (set by default at a random UUID)
- at: creation timestamp 


To filter events, the classes of the events can be used. There are 5 "Intermediate superclasses" (AssetEvent, 
ContractDefinitionEvent, ContractNegotiationEvent, PolicyDefinitionEvent, TransferProcessEvent) of Event. 
Thus, in addition to filtering on a specific event, such as TransferProcessCompleted, it is possible to react to a group of events 
that generally have to do with Assets, ContractDefinition, ContractNegotiation, PolicyDefinition and TransferProcess.

In the example below the subscriber is interested in all events by using the type `Event`. In this case a manual filter with
`instanceOf` the event kind is needed:
 
```java
public class ExampleEventSubscriber implements EventSubscriber<Event> {
    
    public void on(EventEnvelope<Event> event) {
        var payload = event.getPayload();
        if (payload instanceof AssetCreated) {
            // react only to AssetCreated events
        }
    }
    
}
```

To subscribe a particular type of event, a specific class is needed like in this example:

```java
public class ExampleEventSubscriber implements EventSubscriber<AssetCreated> {
    
    public void on(EventEnvelope<AssetCreated> event) {
        // Typed
        AssetCreated payload = event.getPayload();
    }
}
```

This works also for group of events using "intermediate superclasses" such as `AssetEvent`, `ContractDefinitionEvent`, etc.

The dispatcher will take care of calling the right subscribers based on their expressed type.

## Emit custom events
It's also possible to create and publish custom events on top of the EDC eventing system.
To define the event, extend the `Event` class.

> Rule of thumb: events should be named at past tense, as they describe something that's already happened

```java
public class SomethingHappened extends Event {

    private String description;

    public String getDescription() {
        return description;
    }

    private SomethingHappened() {
    }

    public static class Builder  {

        private SomethingHappened event;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            event = SomethingHappened();
        }

        public Builder description(String description) {
            event.description = description;
            return this;
        }

        public SomethingHappened build() {
            Objects.requireNonNull(event.payload.description);
            return event;
        }
    }
}
```

All the data regarding an event should be contained in the `Event` class.

As you may notice, we use the builder pattern to construct objects, as stated in
the [Architecture Principles document](https://github.com/eclipse-edc/docs/blob/main/developer/contributing/coding-principles.md).
The extended builder will inherit all the builder method from the superclass.

Once the event is created, it can be published it through the `EventRouter` component:

```java
public class ExampleBusinessLogic {
    public void doSomething() {
        // some business logic that does something
        var event = SomethingHappened.Builder.newInstance()
                .description("something interesting happened")
                .build();

        var envelope = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(event)
                .build();
        
        eventRouter.publish(envelope);
    }    
}
```

Please note that the `at` field is a timestamp that every event has, and it's mandatory 
(please use the `Clock` service to get the current timestamp).

## Serialization / Deserialization

By default, events must be serializable, because of this, every class that extends `Event` will be serializable to json by default 
(through the `TypeManager` service). 
The json will contain an additional field called `type` that describes the name of the event class. For example, a serialized `EventEnvelope<SomethingHappened>`
event will look like:


```json
{
  "type": "SomethingHappened",
  "at": 1654764642188,
  "payload": {
    "description": "something interesting happened"  
  }
}
```

To make such an event deserializable by the `TypeManager`, is necessary to register the type:

```java
typeManager.registerTypes(new NamedType(SomethingHappened.class, SomethingHappened.class.getSimpleName()));
```

doing so, the event can be deserialized using the `EvenEnvelope` class as type:

```java
var deserialized = typeManager.readValue(json, EventEnvelope.class);
// deserialized will have the `EventEnvelope<SomethingHappened>` type at runtime
```
(please take a look at the [`AssetEventTest`](../../spi/control-plane/asset-spi/src/test/java/org/eclipse/edc/connector/controlplane/asset/spi/event/AssetEventTest.java) class for a serialization/deserialization example)
