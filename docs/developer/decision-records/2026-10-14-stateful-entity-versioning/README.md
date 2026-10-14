# Stateful Entity Versioning

## Decision

We will add an optimistic-locking `version` to `StatefulEntity` and use it to fence writes to transfer processes and
contract negotiations:

- the stores reject a `save` whose version does not match the stored one, with a new `CONFLICT` store failure;
- the callers act on a state change (events, follow-up tasks, command post-actions) only once its `save` succeeded;
- leases stay. The version is a check on top of them, not a replacement.

## Rationale

Transfer processes and contract negotiations are written concurrently by several replicas: by the state machines, by
protocol message handlers and by Management API commands. Leases, stored on the entity row since #6099 and #6101, keep
these writers apart, but they cannot prevent every lost update:

1. runtime A leases an entity, and its work (a DSP message, a data plane call) takes longer than the lease (60 s);
2. runtime B takes the expired lease, processes the entity, saves it and releases the lease;
3. A finishes and saves. The lease is free, so the save passes and overwrites B's newer state with A's stale copy.

A lease only records who holds the entity now. Once B has released it, nothing distinguishes A's stale save from a save
that legitimately takes no lease, like the one of a task executor or the insert of a new entity.

A version detects exactly this case: A's copy carries the version it was read with, B's save has incremented the stored
one, and A's save is rejected.

|                                                        | Lease only                           | Version only                            | Lease and version               |
|--------------------------------------------------------|--------------------------------------|-----------------------------------------|---------------------------------|
| Two runtimes pick the same entity                      | Prevented                            | Both do the work, one save is rejected  | Prevented                       |
| A lease expires during the work, another runtime saves | The late save overwrites newer state | The late save is rejected               | The late save is rejected       |
| A write path that takes no lease                       | Not detected                         | Rejected when stale                     | Rejected when stale             |
| Side effects (DSP messages, events) of a duplicate run | Prevented while the lease lives      | Not prevented, only detected afterwards | Prevented while the lease lives |

Leases cannot be replaced by the version: they stop a second runtime from starting the work at all, while the version
only detects the conflict once the late runtime saves, after its messages have been sent. Row locks
(`SELECT ... FOR UPDATE`) cannot replace them either: the work of a state machine spans asynchronous remote calls, and a
row lock does not outlive its transaction.

A conflict is reported as a failed `StoreResult`, not as an exception, as the stores already do for every other failure
(see [entity store refactoring](../2023-03-02_entity_store_refactoring)). An exception thrown from the success handler of
the state machine's `RetryProcessor` would be treated as an unrecoverable error and terminate the entity.

This builds on an analysis of leases and versions in the Connector and in IdentityHub, which uses `state_time_stamp` as
a version for its issuance processes and holder credential requests.

## Approach

### The version

`StatefulEntity` gets a `long version`:

- `0` means that the entity was never stored;
- the store sets it after every successful `save`, so the same instance can be saved again;
- it is set through the builder and copied by `copy()`. State transitions do not change it;
- it is not exposed in the Management API or in DSP messages.

### Saving

`StateEntityStore.save` keeps its signature and its insert-or-update behaviour. Splitting it into separate create and
update operations, as foreseen by the [entity store refactoring](../2023-03-02_entity_store_refactoring), is left for a
later step.

- A new entity is inserted with version `1`.
- An existing entity is updated only if the stored version equals the entity's version, and only if the lease is free,
  owned by the current runtime or expired. The stored version is incremented.
- When nothing was written, the store reads the row once to report the reason: `ALREADY_LEASED` if another runtime holds
  an unexpired lease, `CONFLICT` otherwise. Saving a new entity whose id already exists is a `CONFLICT` too.
- For contract negotiations, the agreement is written before the negotiation. The lease and version check is therefore
  done before the agreement is written, so that a rejected save writes nothing.

In SQL this is a single upsert:

```sql
INSERT INTO edc_transfer_process (..., version) VALUES (..., :version + 1)
ON CONFLICT (transferprocess_id) DO UPDATE SET ..., version = EXCLUDED.version
WHERE edc_transfer_process.version = :version AND <lease owned or free>
```

`findByIdAndLease` acquires the lease before reading the entity, so that the entity it returns cannot be outdated by a
save committed in between.

The in-memory stores implement the same check, so that the shared store tests cover the conflict for both. Their
queries return copies of the stored entities, like `findById` already does.

### Mapping the conflict

- `ServiceResult.from(StoreResult)`: `CONFLICT` becomes a conflict, which the APIs return as `409`.
  `ServiceResult.fromFailure` maps `CONFLICT` and `ALREADY_LEASED` to a conflict as well (it maps `ALREADY_LEASED` to a
  bad request today).
- `StatusResult.from(StoreResult)`: `CONFLICT` becomes a new `ResponseStatus.CONFLICT`, so that the state machines can
  tell it apart from errors that should be retried or are fatal.
- `CommandResult`: a failed save becomes a conflict.

### Acting only on a saved state

A conflict means that another writer has already moved the entity on. The writer that lost drops its result:

- **state machine (polling):** the processors fire events only after a successful save. On a conflict, the result is
  dropped and logged. The entity is picked up again in its new state by whoever holds it;
- **state machine (task executor):** the processor reports the conflict, the executor creates no follow-up task and
  acknowledges the task. The newer state has its own task;
- **protocol message handlers:** events are fired only after a successful save. A conflict is returned to the
  counterparty as `409`, so that it retries the message;
- **Management API commands:** `postActions` run only after a successful save, and a conflict is returned as `409`.

Side effects that happen before the save cannot be undone: a DSP message or a data plane request is already sent, and a
data address may already be written to the vault. The version does not prevent them; leases keep them rare.

### Scope

- Transfer processes and contract negotiations, in the SQL and in-memory stores.
- Policy monitor entries get the field through `StatefulEntity`, but their store does not check it: only the policy
  monitor writes them.
- IdentityHub can move its checks from `state_time_stamp` to the version.

### Migration

The `version` column is added with `ALTER TABLE ... ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0`.
Existing rows start at `0`, which is the version their entities are read with, so no backfill is needed.

Runtimes of a previous version write without the check and do not increment the version. Conflicts are therefore
detected only once all replicas run the new version.

### Testing

- **Store tests (in-memory and Postgres):**
  - saving a stale copy is rejected with `CONFLICT`;
  - a save increments the version of the stored entity and of the saved instance;
  - inserting an entity whose id already exists is rejected with `CONFLICT`;
  - a save while another runtime holds the lease still fails with `ALREADY_LEASED`.
- **Concurrency test (Postgres):** runtime A's lease expires, runtime B processes and saves the entity, and A's late save
  is rejected while B's state is kept.
- **Unit tests:**
  - result mappings;
  - no events or `postActions` after a failed save;
  - the task executor creates no follow-up task on a conflict.

### Out of scope

- Separate create and update operations on `StateEntityStore`.
- Optimistic checks in the Management API, e.g. an `If-Match` header carrying the version.
- Versioning of policy monitor entries.
