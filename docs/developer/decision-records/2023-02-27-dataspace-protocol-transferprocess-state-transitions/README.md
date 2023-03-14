# Dataspace Protocol TransferProcess state transitions

## Decision

State machine for `TransferProcess` must be changed to align with the [Dataspace Protocol](https://docs.internationaldataspaces.org/communication-guide-v2-draft/overview/readme)

## Rationale

The [new protocol state machine](https://docs.internationaldataspaces.org/communication-guide-v2-draft/transfer-process/transfer.process.protocol)
defines some simple and clear states that handles the lifecycle of a `TransferProcess`,
We need to adapt the EDC `TransferProcess` state machine to fit the provisioning/deprovisioning feature in it.

## Approach

The "provisioning/deprovisioning" feature is not described in the protocol state machine because it is EDC custom,
to make it fit within the state machine we could see it as a wrapping state machine section:

- Provisioning
- Protocol state machine
- Deprovisioning

Doing this the states flow will change, because currently the deprovisioning phase happens before "ENDED/CANCELLED/ERROR",
that are the equivalent states of "COMPLETED/TERMINATED", but in fact the deprovisioning phase should happen after the 
completion or termination.

Another addition needed will be to add the `*ING` states missing: `STARTING`, `COMPLETING`, `TERMINATING`, needing for
reliability.

The new states list should become like:
```
INITIAL(100),
// provisioning
PROVISIONING(200),
PROVISIONING_REQUESTED(250),
PROVISIONED(300),
// protocol state machine
REQUESTING(400),
REQUESTED(500),
STARTING(550),
STARTED(600),
SUSPENDING(650),
SUSPENDED(700),
COMPLETING(750),
COMPLETED(800),
TERMINATING(825),
TERMINATED(850),
// deprovisioning
DEPROVISIONING(900),
DEPROVISIONING_REQUESTED(950),
DEPROVISIONED(1000),
// old states to be deleted
@Deprecated(since = "milestone9") ERROR(-1),
@Deprecated(since = "milestone9") UNSAVED(0),
@Deprecated(since = "milestone9") IN_PROGRESS(600),
@Deprecated(since = "milestone9") STREAMING(700),
@Deprecated(since = "milestone9") ENDED(1100),
@Deprecated(since = "milestone9") CANCELLED(1200);
```
