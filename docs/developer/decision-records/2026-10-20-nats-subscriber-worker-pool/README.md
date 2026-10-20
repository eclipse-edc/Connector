# NATS Subscriber Worker Pool

## Decision

The NATS task subscribers will handle the messages they fetch with a bounded pool of workers, instead of a single
thread. The number of workers is configurable and defaults to `1`.

The subscribers will keep fetching messages in batches, but the number of messages a subscriber holds, i.e. fetched and
not completed yet, will be bounded by a capacity of `workers + prefetch`. A new batch is fetched only once enough of
that capacity is free.

## Rationale

A replica handles the tasks of an entity type (transfer processes, contract negotiations) with a single subscriber
thread: `NatsSubscriber` fetches up to `batch-size` messages (100 by default) and handles them one after the other.
Handling a task is blocking: the task executor waits for the outcome of the remote call (e.g. a DSP message sent to the
counter-party), within the transaction opened by the subscriber. A slow or unreachable counter-party therefore delays
all the tasks behind it on that replica, whatever their participant context or counter-party is: this is the
head-of-line blocking listed among the accepted limitations of the task-based state machine ([#6116]).

Adding replicas does not help, since every replica has the same limit, and the messages of a fetched batch are held by
the replica that fetched them: the in-progress heartbeat keeps their ack wait alive, so they are not delivered to an
idle replica either. Raising `batch-size` makes it worse.

A pool of workers removes the blocking between tasks, up to the number of workers. How many messages are fetched ahead
of the workers is a trade-off:

- **A full batch per fetch, regardless of the free workers.** The messages that are fetched but not started yet are
  parked on the replica: another replica cannot take them, which moves the head-of-line blocking from the thread to the
  replica. They count toward the `MaxAckPending` of the consumer, which is shared by all the replicas. They make the
  shutdown longer, and after a crash they are delivered again only once their ack wait expires.
- **One message per free worker.** Nothing is parked, but when tasks are fast every message costs a fetch round trip.

A bounded prefetch keeps the fetches batched while bounding the parked messages. With the defaults (1 worker, a prefetch
of 10), a subscriber holds at most 11 messages, against up to 100 today, and still handles them sequentially.

## Approach

### Settings

New settings, for the transfer process (`tp`) and contract negotiation (`cn`) subscribers:

| Setting                                | Default | Description                                          |
|----------------------------------------|---------|------------------------------------------------------|
| `edc.nats.{tp,cn}.subscriber.workers`  | `1`     | The number of messages handled concurrently.         |
| `edc.nats.{tp,cn}.subscriber.prefetch` | `10`    | The number of messages fetched ahead of the workers. |

`edc.nats.{tp,cn}.subscriber.batch-size` becomes the maximum number of messages per fetch.

### Flow control

The subscriber keeps a semaphore with `capacity = workers + prefetch` permits, one per message it can hold. The consumer
loop:

1. waits until at least `min(batch-size, ceil(capacity / 2))` permits are free, with a timeout so that it notices when
   the subscriber stops;
2. takes all the free permits and fetches `min(batch-size, permits)` messages, giving back the permits it does not use;
3. submits every message to the worker pool, which gives back its permit once the message is completed.

The handling of a message does not change: it is acknowledged, delayed with `nakWithDelay` or terminated as today. The
held messages are tracked as today, so the in-progress heartbeat keeps the ack wait of all of them alive, whether they
are processed or waiting for a worker.

The workers are platform threads of a fixed pool, instrumented with the `ExecutorInstrumentation` like the other
subscriber threads.

### Concurrency

Two tasks of the same entity can now be handled at the same time on one replica, e.g. a task redelivered after a
failure and the task of the next state. This could already happen between replicas, and leases and versions protect the
entities against it (see [stateful entity versioning](../2026-10-14-stateful-entity-versioning)): one of the two fails
to save and is retried with the usual delay, at which point it finds the entity in another state and is skipped. No
ordering between tasks is needed or added.

### Database connections

A worker holds a database connection for the whole handling of a task, remote call included. A replica can therefore
use up to `workers` connections per subscriber, on top of the API traffic, and the datasource pool must be sized
accordingly. Releasing the connection during the remote call would need the handling of a task to be asynchronous,
and is not part of this decision.

### Shutdown

On `stop()`, the subscriber stops fetching. The messages that no worker has started are released (`nak()` without
delay), so that they are delivered to another replica right away. The messages being processed are given the shutdown
timeout, shared by all of them, to complete before the workers are interrupted. The messages still held are then
released and the connection is closed.

### Future work

- **Per-participant fairness.** Tasks are not scoped by participant context: a participant context with many slow tasks
  can still take all the workers. It could be addressed with a cap on the tasks in flight per participant context,
  which needs the participant context id on the message (e.g. as a header), or with the participant context id in the
  subject.
- **Asynchronous handling.** Not blocking a worker, and a database connection, while the remote call is made.

[#6116]: https://github.com/eclipse-edc/Connector/issues/6116
