# CloudEvents HTTP

This module provides a way to register an http endpoint where the domain events will be sent as soon as they occur, 
respecting the [CloudEvents HTTP spec v1.0.2](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/http-protocol-binding.md).

## Configuration 

| Parameter name                    | Description                                       | Default value       |
|-----------------------------------|---------------------------------------------------|---------------------|
| `edc.events.cloudevents.endpoint` | The http endpoint where the events will be pushed | <mandatory setting> |