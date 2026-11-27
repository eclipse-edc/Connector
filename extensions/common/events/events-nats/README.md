# Publishing Events to NATS

This module provides a way to register an event publisher that publishes EDC events to NATS.

## Enabling the extension

The extension of this module is bound to the `edc.feature.events` feature and is only loaded if it is set to `nats`:

```properties
edc.feature.events=nats
```

If the feature key is not set, the extension is not loaded, even if the module is on the classpath, and no events are
published to NATS. This feature is independent of `edc.feature.statemachine`: publishing events to NATS does not require
the task-based state machine, and vice versa.

## Configuration

| Parameter name           | Description                               | Default value |
|--------------------------|-------------------------------------------|---------------|
| `edc.events.nats.stream` | NATS stream on which events are published |               |