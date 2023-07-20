# Synchronous Commands

## Decision

We're making all the Command interactions with the mutable entities (`ContractNegotiation` and `TransferProcess`) synchronous.

## Rationale

Asynchronous commands brought in a lot of unuseful complexity, they brought also downsides as:
- commands would need to be persisted for reliability
- their result is currently "lost" so there would be the need for some sort of notification mechanism.

Make them synchronous will make everything simpler, it would make them align with the current implementation of the
"protocol" services, that are updating the status synchronously on incoming protocol messages.

## Approach

The `CommandHandler` will return a `CommandResult` that will be successful when the command executed correctly, otherwise
will be a failure.

To avoid conflicts with the state machines, every command handler will need to lease the entity before applying the state
change. If the lease cannot be obtained, the command will fail.
