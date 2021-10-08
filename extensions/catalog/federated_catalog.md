## General thoughts

- The Federated Catalog Cache (FCC) is the one that does the crawling and that goes to FCN's to get their catalog. It
  exposes a query interface to third-party apps such as web UIs
- The Federated Catalog Node (FCN) is hosting the catalog and its sole purpose is to respond to another crawlers'
  inquiries.
- Both the FCC and FCN are thought of as logical components (similar to the EDC). They can be embedded in the EDC via
  extensions, deployed alongside it as standalone runtimes or be deployed completely separate (even in different
  enviroments). It's also possible to deploy a runtime that hosts both an FCN and an FCC.
- we distinguish between a catalog _query_ and a catalog _update request_: the former is coming in from a UI and wants
  to get the entire catalog, the latter is a request issued by the crawler to other FCNs

## About connector resolution

- There should be an interface `FederatedCatalogNodeDirectory` in the `spi` package that provides information about all
  FCNs that are known in a dataspace.
- In a first draft impl this can be a static memory map that the crawler accesses and that it uses to determine its
  fan-out strategy (based on geo-location, last-visited, crawler pool size, etc.)
- Later this could be implemented using HPL (Hyper PLedger) with an appropriate extension

## Terminology

- FCN: Federated Catalog Node - serves its own catalog
- FCC: Federated Catalog Cache - maintains a snapshot of all catalogs in a dataspace. Crawls other FCNs for their
  catalog.
- `Crawler`: a piece of software that periodically issues `update-requests` to other FCNs
- `update-request`: a request to a FCN to get that FCN's catalog
- `Loader`: a piece of software that shoves data into a persistent storage (e.g. hibernate)
- `LoaderManager`: a piece of code that takes data from a queue and feeds it to the `Loader`.
- `QueryEngine`: a module that interprets a `cache-query` and forwards it to specific query adapters
- `cache-query`: a query that the FCC receives from e.g. a connector or any other client
- `QueryAdapter`: receives a query formulated in a general query language and transforms it into whatever, e.g. SQL or a
  Gremlin statement,...

## Architectural and deployment considerations

- `Partition`: is a homogenous subset of the dataspace's overall nodes (e.g. by geographical region)
- `ExecutionPlan`: prescribes how the crawlers should run, i.e. periodically, based on an event, etc.. should be
  JSON-serializable. Contains a queue of work items (which are the target URLs). Should support updating/patching the
  same way kubernetes or terraform do it.
- `PartitionManager`: controls all crawlers in its partition, receives an ExecutionPlan, receives updates to the
  ExecutionPlan. Updates are applied once the crawling is done (i.e. the work queue is empty). sequential updates are
  collated into a final state. The PartitionManager does not know about the amount of Crawlers, it's only job is to
  populate the work-queue once an execution is required.
- `WorkQueue`: Is a list of work items, each of which contains a URL, a protocol name and an error indication.
  `Crawler`: Has access to the work queue, pulls out 1...n items from the queue and forwards it to the protocol adapter
  that corresponds to the work item.

## Use cases for the FCN

**as the FCN I want to:**

- support multiple query protocols such as IDS
- handle queries (i.e. limit/reduce the query result) based on policies
- use the `AssetIndex` to resolve queries
- the `AssetIndex` support pluggable catalog backends such as databases

## Use cases for the FCC

**as the `Crawler` I want to:**

- run periodically
- support multiple update protocols like IDS and "EDC native" (which is yet to be defined)
- issue a catalog update request that goes out to all my protocol adapters
- present my `VerifiableCredential` in every catalog `update-request` (cf. ION Demo)
- put the catalog update response into a queue/ringbuffer
- emit events (e.g. through an `Observable`)

**as the `LoaderManager` I want to:**

- take out a batch of `update-responses` from the queue and feed them to the `Loader`
- emit events
- run independently from the crawler = we should not block each other
- have 1...n `Loaders` that I can delegate out to

**as the `Loader` I want to:**

- insert data into the database
- perform any sort of data sanitization (e.g. no white spaces...)
- perform any sort of data transformation

**as the `QueryEngine` I want to:**

- receive a query in a standardized format (e.g. as a `Query` object) with filters,...
- have 1...n Query adapters
- ask my query adapters whether they can handle an incoming query (`canHandle`) or map query type to a particular
  QueryAdpater
- forward the query to my query adapter(s)
- wait for their result asynchronously, e.g. using a `CompletableFuture` or similar

**as the `QueryAdapter` I want to:**

- implement the concrete persistence backend, e.g. CosmosDB and maintain its credentials
- registers with the QueryEngine based on a query type
- receive the query in a typed format
- transform the query into a specific format (e.g. into SQL, gremlin statements, etc.)
- execute the query against my persistence backend
- return the result asynchronously

