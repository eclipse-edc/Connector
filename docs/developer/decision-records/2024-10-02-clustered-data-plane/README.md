# Clustered data-plane

## Decision

We will make the data-plane being able to run in a clustered environment.

## Rationale

Currently, data-plane cannot run effectively in a clustered environment because:
- there's no way to identify a specific replica that is running a data flow and "terminate/suspend" it
- there's no way to re-start a data flow that was interrupted because the replica crashed

## Approach

We will provide this feature through the `DataPlaneStore` persistence layer.
A `runtimeId` will be added in the `DataFlow`, and it will be set when it gets started with the replica's `runtimeId`.
There will be a configured duration `T` (that can be in milliseconds, seconds at most. A good name for it needs to be found).

### Identify specific replica to suspend/terminate

The now synchronous `suspend`/`terminate` will become asynchronous by putting the `DataFlow` in a `-ING` state like:
- `SUSPENDING`
- `TERMINATING`

For termination (the same logic will be duplicated for suspension), in the `DataPlaneManager` state machine there will be
two new `Processor` registered:
- one filters by `TERMINATING` state and `runtimeId`: it will stop the data flow and transition it to `TERMINATED`
- one filters by `TERMINATING` and by `updatedAt` passed by at least 2/3 times `T`: it will transition the data flow to 
  `TERMINATED` (as cleanup for dangling data flows) 

### Re-start interrupted data flow

Please consider `T` as a configured time duration (milliseconds, seconds at most).

A running data flow will need to update the `updatedAt` field every `T`
In the `DataPlaneManager` state machine, fetches items in `STARTED` with `runtimeId` different from the replica one, 
that have `updatedAt` past by at least 2/3 times `T`.
These data-flows can then be started again

