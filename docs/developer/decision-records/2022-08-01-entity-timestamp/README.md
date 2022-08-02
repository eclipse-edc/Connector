# Add creation and update timestamp

## Decision

All objects in EDC, which can be persisted in a database, should have a `createdAt` timestamp, all objects that
are
_mutable_ should also have a `updatedAt` timestamp. In this document they will be referred to as "business
objects"
/"entities" and "mutable business objects"/"mutable entities" respectively

## Rationale

It should be possible to track the creation time and last-updated time for the aforementioned (mutable)
entities, e.g. for auditing or for displaying purposes in a web frontend.

The `createdAt` timestamp must be immutable. It cannot be changed after the initial object construction. The
`updatedAt` timestamp must be updated everytime the entity is put back into storage. This includes `save`
operations that do not entail an actual change to the object.

## Approach

Both these timestamps are in Epoc milliseconds in Universal Coordinated Time (UTC), so obtaining the current time  
would be done using `Clock.systemUTC().millis()`. This is in compliance with EDC usage of `java.time.Clock`.

The following entities are considered _immutable_:

- `Policy`/`PolicyDefinition`
- `ContractDefinition`
- `Asset`

The following entities are considered _mutable_

- `ContractNegotiation`
- `TransferProcess`

All entities that are effectively immutable, even though the might get re-persisted multiple times, are still
considered _immutable_. They typically cannot exist on their own. For instance a `DataRequest` is always tied to the
`TransferProcess` and cannot be changed once the transfer process has been created. By the same logic, a
`ContractOffer` - once received by either party - is considered immutable and any counter-offer triggers the creation of
a copy.

Similarly, once a `ContractNegotiation` is in the `CONFIRMED` state, i.e. there is a `ContractAgreement` attached to it,
both it and the `ContractAgreement` become _immutable_.

### Data model

I propose extracting the `createdTimestamp` (plus the Builder infrastructure) into a new abstract classes `Entity`
(and renaming it to `createdAt`) that is extended by _all_ entities, mutable and immutable. By default, the `createdAt`
field is initialized with the current UTC epoch in milliseconds.

In addition, the `StatefulEntity` can be extended with a `updatedAt` field:

```java
public abstract class StatefulEntity<T extends StatefulEntity<T>> extends Entity implements TraceCarrier {
    private long updatedAt;

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long epochMillis) {
        updatedAt = epochMillis;
    }
    // ...
}
```

And similar to the `Entity`, the `StatefulEntity`'s Builder would also have a new method that sets the last update. By
default, `updatedAt` would be initialized with `createdAt`.

_Note: the `StatefulEntity` class already has a `Clock` that can be moved up to the `Entity` and be re-used for this
purpose._

### Mutating objects

The `createdAt` field would always be initialized during object creation. The `updatedAt` timestamp would be
updated by the _manipulating class_. That means, whichever class manipulates the `StatefulEntity` is also responsible
for updating the `updatedAt` field. These are:

- `TransferProcessManagerImpl`: right before `transferProcessStore.update()` is called
- `SingleTransferProcessCommandHandler`: could be done in the `else` path of the `handle` method
- `[Consumer|Provider]ContractNegotiationManagerImpl`: right before `negotiationStore.save()` is called. could be
  extracted into the `AbstractContractNegotiationManager`
- `SingleContractNegotiationCommandHandler`: could be done in the `else` path of the `handle` method

### Persisting entities

No action needs to be taken for the CosmosDB store as it is already document-based and persisting another field should
be seamless. For the Postgres implementations the `createdAt` and `updatedAt` fields should be of type `BIGINT`
, for example (`TransferProcess`):

```postgresql

CREATE TABLE IF NOT EXISTS edc_transfer_process
(
    created_at    BIGINT, -- already exists (albeit with a different name)
    updated_at    BIGINT  -- this is new

    -- other columns omitted
);
```
