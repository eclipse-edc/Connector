# Transfer

## Configuration

* `edc.transfer.state-machine.batch-size`
    * the size of the batch of entity fetched for every `TransferProcess` state machine iteration.
    * _Default value_: 5
* `edc.transfer.state-machine.iteration-wait-millis`
    * the iteration wait time in milliseconds on the state machine while creating a `WaitStrategy` variable
    * _Default value_: 5000
* `edc.transfer.state-machine.lease-duration`
    * how long a runtime keeps the lease on a `TransferProcess` while processing it, in ISO-8601 duration format. It
      must be longer than the slowest processing step (e.g. a protocol message dispatch including HTTP timeouts and
      retries), otherwise the `TransferProcess` can be processed again, by another runtime or by the same one.
    * _Default value_: PT60S