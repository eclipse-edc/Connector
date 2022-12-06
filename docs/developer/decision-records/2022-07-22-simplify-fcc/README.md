# Technical concept to simplify the FederatedCatalog

## Decision

We will simplify the FederatedCatalog in the aspects outlined in this document, because experience shows that the
initial complexity and versatility is in fact not needed.
The intended audience are developers, who are writing code for the FC and adopters who use and operate an FC.

## Rationale

The Federated Catalog was originally designed with the greatest flexibility and versatility in mind. For instance
it has a `PartitionManager`, that allows for partitioning a dataspace, e.g. by geographic areas.

Similarly, all components such as work queues, worker threads (`Crawler`s) and the persistence pipeline are decoupled
using queues and parallel processing to enable the utmost independence and extensibility.

Practical applications so far have shown that all this complexity is not needed, rather, it makes the FC architecture
hard to understand and hard to maintain.

Reducing complexity of the code base will be beneficial in several ways: code paths are easier to understand, the code
becomes easier to test and is less prone to implementation errors.

## Approach

We therefore propose the following changes and simplifications:

### 1. Remove the `PartitionManager`

Partitioning would likely be done using a different approach, such as deployment paradigms, so the partition manager can
be deleted.

This means the `FederatedCatalogCacheExtension` directly instantiates the `ExecutionPlan`, and starts it.

### 2. Make the `WorkItemQueue` short-lived

When the `ExecutionPlan` is run, it fetches all `FederatedCacheNode` elements from the `FederatedCacheNodeDirectory`,
instantiates a new `WorkItemQueue` and inserts the elements into it. The crawlers then set off and drain the queue. This
means that a new queue is used for every crawl-run (assuming periodic crawling). Aside from (small) resource
improvements, it makes the entire FC more stateless.

### 3. Make the `Crawler` short-lived

Currently, crawlers are instantiated once and re-used on every crawl-run. Between runs, they needlessly poll the queue.
With this proposal I suggest that the `ExecutionPlan` directly instantiates the crawlers (e.g. through
a `Supplier` or a `Function`) and sets them off in its `run` method, passing the next `WorkItem` as argument.

Consequently, the `ExecutionPlan` maintains a list of `Crawlers`, tracks their state (running vs finished), and removes
them from its internal list once they are done. It then instantiates new ones until the `WorkItemQueue` is empty. That
way, crawlers can be simplified greatly, because they do not have to implement "polling the queue" anymore, thus
reducing code complexity. With this, they will also mimic the semantics of a stateless "worker" more closely.

_Note: this would mean the logic to poll the queue gets moved into the `ExecutionPlan` at which point we could think
about introducing a new collaborator object for the `ExecutionPlan`, an `ExecutionManager` of sorts._

### 4. Have `Crawler`s directly write to the `FederatedCacheStore`

Currently, every `Crawler` puts its result (i.e. a `UpdateResponse`) into yet another queue, from which the `Loader`
dequeues it and forwards it to the store.
Since the persistence abstraction is handled by the store anyway, there really is no need for the indirection through
one (or potentially multiple) loaders anymore.