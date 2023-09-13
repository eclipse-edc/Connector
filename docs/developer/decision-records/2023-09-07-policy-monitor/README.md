# Generic Properties

## Decision

A `PolicyMonitor` extension component will be implemented.

## Rationale

Some transfer types, such as streaming never reach the `COMPLETED` state, so they need to be terminated whenever the
`Policy` on which the participants agreed is not valid anymore.

## Approach

This component will be implemented using the same model as the state machine we're using for `TransferProcess`:

- The ingestion will be done with a listener on the `TransferProcessStarted` event, that will add an entry to a `PolicyMonitorStore`.
- The state machine then will continuously fetch the oldest entries leasing them, check the related policy. 
  - If the check fails the TP will get completed through a service call (that could be either embedded in the control-plane 
    or remote through a rest call) and the entry will be removed from the `PolicyMonitorStore`.
  - otherwise the lease will be broken and the policy be verified again in the next run.

With "leasing" we mean that the entity will be reserved by a connector instance that will be the only one allowed to modify it
until the lease gets broken, and this can happen by updating it or after a certain amount of time.

This mechanism will permit to scale the process to multiple instances.

The `PolicyMonitor` will be deployable embedded in the control-plane or separately in a standalone runtime.
The `PolicyMonitorStore` will have a in-memory implementation for testing scenarios and a sql (postgres) one.
