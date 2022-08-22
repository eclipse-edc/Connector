# Contract

## Configuration

| Parameter name                                      | Description                                                                                               | Mandatory | Default value |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------|-----------|---------------|
| `edc.negotiation.consumer.state-machine.batch-size` | the size of the batch of entity fetched for every consumer `ContractNegotiation` state machine iteration. | false     | 5             |
| `edc.negotiation.provider.state-machine.batch-size` | the size of the batch of entity fetched for every provider `ContractNegotiation` state machine iteration. | false     | 5             |
| `edc.negotiation.consumer.send.retry.limit`         | the limit of retries in case of consumer `ContractNegotiation` sending failure.                           | false     | 7             |
| `edc.negotiation.provider.send.retry.limit`         | the limit of retries in case of provider `ContractNegotiation` sending failure.                           | false     | 7             |
| `edc.negotiation.consumer.send.retry.base-delay.ms` | the base ms delay value for consumer `ContractNegotiation` sending retrial.                               | false     | 100           |
| `edc.negotiation.provider.send.retry.base-delay.ms` | the base ms delay value for consumer `ContractNegotiation` sending retrial.                               | false     | 100           |
