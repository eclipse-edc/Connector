# Generic Properties

## Decision

A `PolicyMonitor` extension component will be implemented.

## Rationale

Ongoing transfers like "streaming" one, where they actually don't get `COMPLETED`, need to be terminated whenever the
`Policy` on which the parts agreed on is not valid anymore.

## Approach

This component will be implemented using the same model as the state machine we're using for `TransferProcess`:

- The ingestion will be done with a listener on the `TransferProcessStarted` event, that will add an entry to a `PolicyMonitorStore`.
- The state machine then will continuously fetch the oldest entries leasing them, check the related policy. 
  - If the check fails the TP will get completed through a service call (that could be either embedded in the control-plane 
    or remote through a rest call) and the entry will be removed from the `PolicyMonitorStore`.
  - otherwise the lease will be broken and the policy be verified again in the next run.

This mechanism will permit to scale the process to multiple instances.

The `PolicyMonitor` will be deployable embedded in the control-plane or separately in a dedicated runtime.
The `PolicyMonitorStore` will have a in-memory implementation for testing scenarios and a sql (postgres) one.
