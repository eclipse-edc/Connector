# Transfer

## Configuration

* `edc.transfer.state-machine.batch-size`
    * the size of the batch of entity fetched for every `TransferProcess` state machine iteration.
    * _Default value_: 5
* `edc.transfer.contract-service.iteration-wait`
    * the iteration wait time in milliseconds on the state machine while creating a `WaitStrategy` variable
    * _Default value_: 5000