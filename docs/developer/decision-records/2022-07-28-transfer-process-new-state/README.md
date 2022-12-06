# TransferProcess new "Provisioning Requested" state

## Decision

Since the provisioning phase became "async" with the introduction of the `http-provisioner` extension,
there's the need to introduce a new state that describes a transfer process for which the provisioning has been requested
but it hasn't been completed yet.

## Rationale

The classic provision method (see `S3BucketProvisioner` or `ObjectStorageProvisioner`), despite providing `async` interface,
works in a synchronous way: when the `CompletableFuture` returned by the provisioner completes, this means that the actual
provisioning process completed and the `StateMachineManager` can fetch that `TransferProcess` and continue its processing.
The `http-provisioner` works in a different way: when the `CompletableFuture` completes, this means that the provisioning
has been requested, but it will be completed in the future. At that point the `TransferProcess` should stay in a state
that would make the `StateMachineManager` ignore it, as it would be up to the external provisioner to complete the provisioning
calling back the connector.

The same rationale goes for the `deprovisioning` phase.

## Approach

As we already did for the execution of "asynchronous operations" like contract negotiation, the solution would be to add
a new state that is not processed by the `StateMachineManager`, in this case the state sequence would go:
- `PROVISIONING(200)` (request sent to provisioner) -> `PROVISIONING_REQUESTED(250)` (while the provisioner is working) -> `PROVISIONED(300)`
- `DEPROVISIONING(900)` (request sent to deprovisioner) -> `DEPROVISIONING_REQUESTED(950)` (while the deprovisioner is working) -> `DEPROVISIONED(1000)`

When the `provisionManager.provision` result completes and in the resource manifest there's at least one `ProvisionResponse`
that's `inProcess` then the `TransferProcess` will be put in that `*_REQUESTED` state.

The same approach goes for the `deprovisioning phase`