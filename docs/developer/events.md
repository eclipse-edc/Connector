# Events

EDC provides an eventing system that permits the developer to write extensions that could react to events that are 
emitted from the core of the EDC and also emit custom events.

## Subscribe to events
The entry point for event listening is the `EventRouter` component, on which an `EventSubscriber` can be registered.

Extension example:
```java
public class ExampleEventSubscriptionExtension implements ServiceExtension {
    @Inject
    private EventRouter eventRouter;

    @Override
    public void initialize(ServiceExtensionContext context) {
        eventRouter.register(new ExampleEventSubscriber());
    }
}
```

Then the `EventSubscriber` subscription will receive all the events emitted from the EDC and react to them:
```java
public class ExampleEventSubscriber implements EventSubscriber {
    
    public void on(Event event) {
        // react to event    
    }
    
}
```

All the events are subclass of the `Event` abstract class so to filter events it's enough to check the type at runtime.
```java
public class ExampleEventSubscriber implements EventSubscriber {
    
    public void on(Event event) {
        if (event instanceof AssertCreated) {
            // react only to AssertCreated events
        }
    }
    
}
```

## Emit custom events
It's also possible to create and publish custom events on top of the EDC eventing system.
To define the event, extend the `Event` class adding the fields that would describe the events.
> Rule of thumb: events should be named at past tense, as they describe something that's already happened
```java
public class SomethingHappened extends Event {
    private String description;

    private SomethingHappened() {
    }
    
    private String getDescription() {
        return description;
    }

    public static class Builder extends Event.Builder<SomethingHappened> {

        public static Builder newInstance() {
            return new Builder(new SomethingHappened());
        }

        private Builder(SomethingHappened event) {
            super(event);
        }

        public Builder description(String description) {
            event.description = description;
            return this;
        }

        public AssetCreated build() {
            super.build();
            Objects.requireNonNull(event.description);
            // this validation helps to catch up missing properties in the test phase,
            // but isn't supposed to fail in a production environment, so it's not mandatory.
            return event;
        }
    }
}
```

As you may notice, we use the builder pattern to construct objects, as stated in the [Architecture Principles document](../architecture/architecture-principles.md).
The extended builder will inherit all the builder method from the superclass.

Once the event is created, it can be published it through the `EventRouter` component:
```java
public class ExampleBusinessLogic {
    public void doSomething() {
        // some business logic that does something
        var event = SomethingHappened.Builder.newInstance()
                .description("something interesting happened")
                .at(clock.millis())
                .build();
        
        eventRouter.publish(event);
    }    
}
```
Please note that the `at` field is a timestamp that every event has, and it's mandatory 
(please use the `Clock` service to get the current timestamp).

## Serialization / Deserialization

By default, events must be serializable, because of this, every class that extends `Event` will be serializable to json by default 
(through the `TypeManager` service). 
The json will contain an additional field called `type` that describes the name of the event class. For example, a serialized `SomethingHappened`
event will look like:
```json
{
  "type": "SomethingHappened",
  "at": 1654764642188,
  "description": "something interesting happened"
}
```
Given that, the `type` field should not be used by extending classes.

To make such an event deserializable by the `TypeManager`, is necessary to register the type:
```java
typeManager.registerTypes(new NamedType(SomethingHappened.class, SomethingHappened.class.getSimpleName()));
```

doing so, the event can be deserialized using the `Event` superclass as type:
```
var deserialized = typeManager.readValue(json, Event.class);
// deserialized will have the `SomethingHappened` type at runtime
```