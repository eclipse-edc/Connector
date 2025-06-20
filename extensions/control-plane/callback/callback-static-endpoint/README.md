# Static Callbacks Extension

This extension provides an implementation and configuration for static callbacks outlined in this [DecisionRecord](https://github.com/eclipse-edc/Connector/tree/main/docs/developer/decision-records/2023-02-28-processing-callbacks#static-endpoints) 
The callbacks are built on top of the EDC event system. When an event is fired in EDC all registered callbacks will be invoked.

## Configuration

| Parameter name                        | Description                                                                                     | Mandatory | Default value |
|---------------------------------------|-------------------------------------------------------------------------------------------------|-----------|---------------|
| `edc.callback.<name>.uri`             | The address used to dispatch a callback notification                                            | true      | null          |
| `edc.callback.<name>.events`          | The comma separated events on which the callback should be invoked.                             | true      | null          |
| `edc.callback.<name>.transactional`   | If the callback should be invoked inside the transactional boundaries of the configured events. | false     | false         |
| `edc.callback.<name>.auth-key`        | Transport specific header to use for sending an authentication value.                           | false     | null          |
| `edc.callback.<name>.auth-code-id`    | The id for the authorization value that will be resolved by the Vault.                          | false     | null          |


Example configuration with two callbacks:

```bash
edc.callback.endpoint1.uri=http://localhost:8080/hooks
edc.callback.endpoint1.events=contract.negotiation,transfer.process
edc.callback.endpoint1.transactional=true

edc.callback.endpoint2.uri=http://localhost:8081/hooks
edc.callback.endpoint2.events=contract.negotiation.finalized,transfer.process.completed
edc.callback.endpoint2.transactional=false
edc.callback.endpoint2.auth-key=X-API-KEY
edc.callback.endpoint2.auth-code-id=mysecret
```

The first one `endpoint1` will be invoked transactionally in all events emitted from the `ContractNegotiation` and `TransferProcess` state machines.

The second one will only be invoked asynchronously only when a `ContractNegotiation` is finalized and a `TransferProcess` is completed.
