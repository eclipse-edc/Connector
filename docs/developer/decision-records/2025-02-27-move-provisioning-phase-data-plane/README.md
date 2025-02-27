# Move transfer provisioning phase in data-plane

## Decision

We will move the transfer provisioning and deprovisioning phases in the Data-Plane

## Rationale

At the moment the provisioning and deprovisioning phases are managed by the Control-Plane, specifically in the
`TransferProcessManager`.
This is not optimal because it's making the Control-Plane to need knowledge on how to deal with specific data flow protocols,
while it should be agnostic.

Making the Data-Plane handling those will ensure better responsibilities separation and simplify the transfer process logic.

## Approach

The data-plane signaling interface will have an additional method (`prepare`) and, with the other methods (`start`,
`suspend`, `terminate`) will have a common behavior, on which the `provision/deprovision` async operation will be
eventually triggered on the call, and the caller (the control plane) will be immediately aware if the async operation
has been triggered, if not the `TransferProcess` state machine can continue with the standard flow, otherwise the entity
will be put in an intermediate state `*-REQUESTED`, waiting for a callback from the data plane.
If the callback is successful, the state machine will continue the standard flow, otherwise there will be a retry mechanism
put in place. The state machine will also monitor "stale" `*-REQUESTED` states, to prevent potential data-plane
failures.

The flow pattern in the different cases has been represented in this table:

| Starting state | Type       | Signaling Method | Async operation | State if async operation happen | State if async operation NOT happen |
|----------------|------------|------------------|-----------------|---------------------------------|-------------------------------------|
| `INITIAL`      | `CONSUMER` | `prepare`        | provisioning    | `PREPARATION_REQUESTED`         | `REQUESTING`                        |
| `INITIAL`      | `PROVIDER` | `start`          | provisioning    | `STARTUP_REQUESTED`             | `STARTING`                          |
| `STARTED`      | <both>     | `suspend`        | deprovisioning  | `SUSPENSION_REQUESTED`          | `SUSPENDING`                        |
| `STARTED`      | <both>     | `terminate`      | deprovisioning  | `TERMINATION_REQUESTED`         | `TERMINATING`                       |

The `DataFlow` will get the new `PROVISION*` and `DEPROVISION*` states and the state machine will treat them the same
way the `TransferProcessManager` is treating them.


### Plan

To avoid disruptions for users during upgrade the refactoring will be made in incremental way in this order:
- add provisioning process on the `DataPlaneManager` with additional states and transitions (the services used will be
  the same, switching `TransferProcess` to `DataFlow` as target entity)
- add deprovisioning process on the `DataPlaneManager` with additional states and transitions (the services used will be
  the same, switching `TransferProcess` to `DataFlow` as target entity)
- remove provisioning process from the `TransferProcess`
- remove provisioning process from the `TransferProcess`
- deprecation of the `*PROVISION*` states in `TransferProcessStates, to be deleted after some versions

### Assumptions

- the provisioner can modify the source/destination addresses, it is expected that the eventual new addresses are supported
  by the data-plane instance that took care of the provisioning, otherwise it will cause failure in the transfer startup.
