# Cosmos DB lease mechanism

## Decision

Cosmos DB stores for managing contract negotiations and data transfers use a lease mechanism to control the processing of the items.

## Rationale

If a connector fails after receiving a request, a process needs to be picked up by another connector instance. This is specially important when deploying to Kubernetes, since pods can be restarted at any time (e.g. VM autoscale). To ensure a resilient behavior a lease mechanism must be introduced in the transfer and contract negotiation processes.

## Approach

Cosmos document is extended with lease information: a lease holder, time when the lease was acquired and a lease duration.

Lease acts as an exclusive lock: a party leasing a document owns an exclusive lock until the lease expires or it has been explicitly broken. Any attempt to acquire or break the lease on a document that has been leased by someone else results in an exception. Currently, the lease expiration time is hardcoded to 60 seconds.

Cosmos DB stores are using 2 stored procedures that handle the lease logic:

1. [`lease.js`](/extensions/azure/cosmos/cosmos-common/src/main/resources/lease.js) - queries the database to look for an item and if an item is found applies a lease on it. It's used to acquire the lock on single item in the database before an update and to remove the lock after.

```java
   leaseContext.acquireLease(process.getId());
   failsafeExecutor.run(() -> cosmosDbApi.saveItem(document));
   leaseContext.breakLease(process.getId());
```

1. [`nextForState.js`](/extensions/azure/cosmos/cosmos-common/src/main/resources/nextForState.js) - queries the database to look for all items that has no lease or expired lease. It applies a new lease on all returned items. This stored procedure is used to get a batch of elements that are next to be processed for given state.

More about stored procedures in Cosmos DB: [stored procedures doc](https://docs.microsoft.com/rest/api/cosmos-db/stored-procedures).
