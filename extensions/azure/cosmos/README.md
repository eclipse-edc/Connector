# Storage implementations based on CosmosDB

CosmosDB is a NoSQL database (formerly known as DocumentDB) that offers geo-replication, geo-distribution, several
[consistency levels](https://docs.microsoft.com/en-us/azure/cosmos-db/consistency-levels), of which we're
using `Session` setting, and a variety of APIs (SQL, Mongo, Table, etc.).

## General structure

Generally there should be one submodule for every store implementation based on CosmosDB, postfixed with `-cosmos`,
e.g. `assetindex-cosmos` or `policy-store-cosmos`. Those submodules should contain the implementation for the
store as well as a `ServiceExtension` implementor that registers the store.

Furthermore there is some common code, such as SQL statement interpreters and -converters, the "leasing" mechanism (see
[Pessimistic Locking](README.md#pessimistic-locking)), which is located in the `cosmos-common` module.

## Why the `CosmosDbApi`?

In order to hide some complexities of the Java SDK for CosmosDB we implemented a simpler and more tailored experience
for CosmosDB, called the `CosmosDbApi` which offers basic read-write methods.

The implementation of that interface targets one particular container, so in an application that uses EDC uses several
CosmosDB-based stores, there would have to be several instances of the `CosmosDbApiImpl`, each targeting one container.

Typically, the `CosmodDbApiImpl` gets instantiated in the accompanying extension class and then gets passed into the
store
implementation.

From a pure technical perspective, using the `CosmosDbApi` abstraction layer is not necessary - one could also use
the `CosmosClient` directly. However, for most use cases the `CosmosDbApi` offers convenient methods and hides some of
the complexity of the `CosmosClient`.

For more information please refer to the Javadoc of `CosmosDbApi` and `CosmosDbApiImpl`.

## Using document wrappers

CosmosDB can essentially store any arbitrary JSON documents. However, for the purposes of EDC we've introduced a wrapper
object, named the `CosmosDocument` and as a subclass the `LeaseableCosmosDocument`. Both offer a unified way of
interacting with CosmosDB documents.

All documents stored using EDC's CosmosDB stores must have:

- an ID
- a partition key
- a `wrappedInstance`: by default this is the JSON representation of whatever object should be stored in CosmosDB.

### A word on partition keys

Partition keys are used to help CosmosDB determine how data should be partitioned physically.
While CosmosDB suggests
to [use an item's ID as partition keys](https://docs.microsoft.com/en-us/azure/cosmos-db/partitioning-overview#choose-partitionkey)
,
this will simply not be possible in our use case, because [stored procedures](README.md#stored-procedures), which we use
for [pessimistic locking](README.md#pessimistic-locking),
only work _within_ the same partition.

We therefore generally recommend to use a static partition key for all items in a particular store, especially if there
are stored procedures involved.

## SQL statement creation

CosmosDB offers an SQL-like API, which we use for queries. The `cosmos-common` module offers an easy way to fluently
create SQL statements from a `Criterion`, or rather, a `List<Criterion>`. The entrypoint to every SQL statement should
be the `SqlStatement`
class ([here](cosmos-common/src/main/java/org/eclipse/dataspaceconnector/azure/cosmos/dialect/SqlStatement.java)).

## Pessimistic locking

Some CosmosDB-based stores that are included in EDC, more specifically, the `CosmosTransferProcessStore`
and `CosmosContractNegotiationStore`, require that items are locked against simultaneous access. For example, when
the `TransferProcessManager` fetches the next couple of `TransferProcess` items for processing, it needs to be
guaranteed that no other process
modifies the same item in the meantime. This would lead to corrupted states and therefore invalid state transitions.
The same is true for the `ContractNegotiationManager` (or rather: it's subtypes).

As a consequence, the `read`-operation on the DB must lock the item to guard against illegal modifications. In the
context
of EDC we call this a `"lease"`.

Clients such as the `CosmosTransferProcessStore` reference a `LeaseContext`, which allows them to acquire the lease
_explicitly_ by calling `LeaseContext#acquireLease()`, or _implicitly_.

### Explicit leases

The explicit case is the simpler one, as it simply updates the `Lease` property of the document. This operation may
fail, if the item
in question is already leased. Note that the CosmosDB's consistency level may have an influence on this behaviour.

We generally recommend using explicit leases for `update` operations. Every `acquireLease()` operation should be
followed by a
`breakLease()` operation, similar to Java's `WriteLock` class.

### Implicit leases

Implicit leases are more difficult, as they require a more sophisticated write operation. Let's look at a situation,
where a batch of items should be obtained from the database, and in the same transaction those objects should also be
guarded
against competing access (the `TransferProcessStore#nextForState()` would be such a situation).

There, we have multiple database operations:

- `SELECT` all items that are not leased and satisfy a particular condition
- `UPDATE` every single item in the selected set -> write lease

We need to "lease and return" the items in one fell swoop.
In relational SQL databases we could use transactions of some sort, or possibly even some variant
of row-level locks, e.g. a `SELECT ... FOR UPDATE` statement, the concrete syntax of which will depend largely on the
dialect.

In CosmosDB, the only way to achieve atomic operations is through UDFs (User-defined functions) and SPROCs (Stored
Procedures), the latter of which is far more powerful and versatile.

This means that for stores that require that sort of pessimistic locking, we've implemented (and recommend the use of)
SPROCs.

Both _explicit_ and _implicit_ leases expire after some time (default = 60 seconds), at which point they can be
re-leased.

## Stored Procedures

**Please be aware, that if the SPROCs mentioned below are not uploaded to the CosmosDB container, the CosmosDB
implementations provided by EDC won't work! Most likely logs will show repeated 404 errors.**

Currently, there are two different stored procedures available for use. Both are written in Javascript and are
provided as resources in the `cosmos-common` module.

- [lease.js](cosmos-common/src/main/resources/lease.js): used for explicit leases. Simply updates the `lease` property
  of a `LeaseableCosmosDocument`.
  Will fail if the lease cannot be acquired.
- [nextForState.js](cosmos-common/src/main/resources/nextForState.js): used for explicit leases when performing "lease
  and return" operations.

## Module-specific configuration

Please find the specific configuration values for each module in the respective sub-folder:

- [AssetIndex](assetindex-cosmos/README.md)
- [ContractDefinitionStore](contract-definition-store-cosmos/README.md)
- [ContractNegotiationStore](contract-negotiation-store-cosmos/README.md)
- [FederatedCache Node directory](fcc-node-directory-cosmos/README.md)
- [PolicyStore](policy-store-cosmos/README.md)
- [TransferProcessStore](transfer-process-store-cosmos/README.md)