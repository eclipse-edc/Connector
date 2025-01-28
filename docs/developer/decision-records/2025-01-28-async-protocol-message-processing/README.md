# Asynchronous protocol message processing

## Decision

On DSP interactions, the computation of potential long-running operations will happen asynchronously, so one participant
that sends, e.g. a `TransferTerminationMessage` can consider it terminated in the very moment it receives an ACK, also
if asynchronous computation is needed on the counter-party side.

## Rationale

Currently, in the `*ProtocolService` we are doing computation that could potentially block the counter-party, e.g. the
suspension of a transfer process. These kind of operations should be done asynchronously by the `*Manager`. 
The `*ProtocolService` should just:
- validate the incoming message.
- if not valid, return `ERROR`
- if valid:
  - if async computation is needed: put the entity in the `*ING` status and let the `*Manager` do the job
  - if async computation is not needed: put the entity in the `*ED` status
  - in every case, return the `ACK`, transactional with the state change

## Approach

Move all the asynchronous computations from the `*ProtocolService` class to the `*Manager`.

Additionally, we'll change the way we are returning the state in the `GET` endpoints for Contract Negotiations and
Transfer Process, as the status returned should map to the protocol defined status. So the `*ING` status must be mapped
to the related `*ED` status.
E.g: if the entity is in the `TERMINATING` status, the `TERMINATED` status should be put in the response.
