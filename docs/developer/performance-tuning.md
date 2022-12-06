# Performance Tuning

Out of the box the EDC provides a set of default configurations that aim to find a good balance for performances.
The extensibility nature of the EDC permits the user to configure it deeply.

Here will be showed how performance could be improved.

## State Machines
At the core of the EDC there are different [`State Machines`](state-machine.md), and their configuration is crucial to
reach the best performances.

### Settings
The most important settings for configuring a state machine are:
- `iteration-wait`: the time that the state machine will pass before fetching the next batch of entities to process in 
                    the case in the last iteration there was no processing; Otherwise no wait is applied.
- `batch-size`: how many entities are fetched from the store for processing by the connector instance. The entities are
                locked pessimistically against mutual access, so for the time of the processing no other connector 
                instances can read the same entities.

### How to tune them
In the control-plane there are 3 state machines:
- `negotiation-consumer`: the state machine that handles the contract negotiations from a consumer perspective
- `negotiation-provider`: the state machine that handles the contract negotiations from a provider perspective
- `transfer-process`: the state machine that handles the transfer processes

For every state machine you can set the `iteration-wait` (actually for the `negotiation-*` there's a single setting 
used for both) and the `batch-size`, so the settings (and their default value) are:

`edc.negotiation.state-machine.iteration-wait-millis` = 1000
`edc.negotiation.consumer.state-machine.batch-size` = 20
`edc.negotiation.provider.state-machine.batch-size` = 20
`edc.transfer.state-machine.iteration-wait-millis` = 1000
`edc.transfer.state-machine.batch-size` = 20

Thus, by default all the control-plane state machines will have an iteration of 1 second if no
entities are found/processed. There will be no wait but the next iteration will start as soon as all the entities are 
processed. At every iteration 20 entities are fetched.

Changing these values you could tune your connector, for example reducing the `iteration-wait` will mean that the state
machine will be more reactive, and increasing the `batch-size` will mean that more entities will be processed in the
same iteration. Please note increasing `batch-size` too much could bring to longer processing time in the case that 
there are a lot of different entities and that reducing `iteration-wait` too much will make the state machine spend more
time in the fetch operation.

If tweaking the settings doesn't give you a performance boost, you can achieve them through horizontal scaling.
