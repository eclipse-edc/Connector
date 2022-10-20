# Logging

A comprehensive and consistent way of logging is a crucial pillar for operability. Therefore, the following rules should be followed:

## Logging component

Logs must only be produced using the [`Monitor`](../../spi/common/core-spi/src/main/java/org/eclipse/edc/spi/monitor/Monitor.java) service, 
which offers 4 different log levels:

### `severe` 
> Error events that might lead the application to abort or still allow it to continue running.

Used in case of an unexpected interruption of the flow or when something is broken, i.e. an operator has to take action. 
e.g. service crashes, database in illegal state, ... even if there is chance of self recovery.

### `warning`
> Potentially harmful situations messages.

Used in case of an expected event that does not interrupt the flow but that should be taken into consideration. 
 
### `info`
> Informational messages that highlight the progress of the application at coarse-grained level.
 
Used to describe the normal flow of the application.
 
### `debug` 
> Fine-grained informational events that are most useful to debug an application.
 
Used to describe details of the normal flow that are not interesting for a production environment.

## What should be logged
- every exception with `severe` or `warning`
- every `Result` object evaluated as `failed`: 
  - with `severe` if this is something that interrupts the flow and someone should take care of immediately
  - with `warning` if this is something that doesn't interrupt the flow but someone should take care of, because it could give worse results in the future
- every important message that's not an error with `info`
- other informative events like incoming calls at the API layer or state changes with `debug`

## What should be not logged

- secrets and any other potentially sensitive data, like the payload that is passed through the `data-plane`
- an exception that will be thrown in the same block
- not strictly necessary information, like "entering method X", "leaving block Y", "returning HTTP 200"
