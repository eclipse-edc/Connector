# Technical concept DPF Selector

This document presents the technical concept for the "DataPlaneFramework Selector", henceforth "DPF Selector" or "DPFS"
for short. In typical clustered environments (Kubernetes) we anticipate a multitude of connector runtimes and DPF
runtimes, scaling independently of each other, depending on a particular use case. Also, the DPFs may have different
configurations, so they may not even be in the same replica set (in Azure this would be Availability Sets vs. Scale
Sets).

Further, we assume that every connector has access to multiple DPF runtimes, even if they are not homogenous replicas.
For example, some deployments may choose to have one dedicated DPF per geographical region, or even per `DataSink`
/`DataSource` pair.

Whenever a data transfer request hits the connector, it eventually will delegate the transfer to the DPF. For that, it
needs to know _which DPF to delegate to_. In simple deployments, there may just be one single DPF, so the DPF selector
is essentially a preconfigured value, but in more complex scenarios sophisticated algorithms may be employed to select
the best-fitting DPF.

This is not dissimilar to how load-balancers work, one key difference being that the DPF selector will distribute the
request based on not just the load situation, but also based on request metadata or even functional requirements (e.g. "
must be able to store in Azure BlobStore").

Another key difference to (most) load balancers is the modularity and flexibility of the DPF selector: it can be
embedded into another process (e.g. a connector), standalone, it can handle different storage and API technologies, it
itself could theoretically run clustered, etc.

## Terminology

- DPF: one DataPlaneFramework runtime instance
- CP: one ControlPlane instance, often used synonymous to "connector"
- connector: one connector runtime
- selector: one DPF selector runtime, either embedded or standalone
- DataSource: one particular implementation that the DPF uses to fetch data from (e.g. Amazon S3)
- DataSink: one particular implementation of a data destination (e.g. AzureBlob)
- Seeding: the process of pre-populating a database. Often used synonymous to "loading".
- Feature pack: a collection of Java modules that fulfill one particular purpose, typically accompanied by a BOM (
  bill-of-materials)

## Proposed solution

We propose to create another "feature pack" in the EDC repository, essentially a meta module
named `:extensions:data-plane-selector`, which will contain (at least) the following submodules:

- `selector-spi`: for interfaces and extensibility points such as store, strategy
- `data-plane-selector-api`: REST API
- `selector-core`: common code, default implementations for store and strategies

Furthermore, there should be a `selector-launcher` in the `launchers` directory containing a build file for standalone
deployment.

Centrally to the DPF selector is an interface `DataPlaneSelector` whose purpose is to select a DPF instance based on an
incoming `DataRequest` and an optional `SelectionStrategy`:

```java
public interface DataPlaneSelector {

    default DataPlaneInstance select(DataRequest dataRequest) {
        return select(dataRequest, new RandomSelectionStrategy());
    }

    DataPlaneInstance select(DataRequest dataRequest, SelectionStrategy strategy);
}
```

The main business object is the `DataPlaneInstance` which represents one particular DPF instance with circa the
following signature:

```java
public interface DataPlaneInstance {
    boolean canHandle(DataRequest request);

    URL getUrl(); //returns the url+path to the DataPlane API

    int turnCount(); //how often selected

    long lastActive(); //when selected last

    Map<String, Object> getProperties(); //extensible properties
}
```

Thus, the `DataPlaneInstance` itself determines whether it can support a particular data request, based on various
capabilities such as its `DataSink` and `DataSource` implementations, streaming capabilities, etc.

Requests come in either through an API or an SPI hit the `DataPlaneSelector`, which then filters its list
of `DataPlaneInstace` records and selects the best fitting DPF based on a strategy. By default, selection is made at
random.

### API/SPI considerations

We'll provide both a REST API and a Java SPI (i.e. the `DataPlaneSelector`), so that the DPF selector can run embedded
in another process, or as a standalone runtime. Both of those should offer the same functionality, which is:

- `get-instance`: selects the best-fit DPF for a `DataRequest`, optionally accepts a `SelectionStrategy`
- `add-instance`: adds a `DataPlaneInstance` to the internal store
- `remove-instance`: removes a `DataPlaneInstance` from the internal store

Both the REST API and the SPI implement these use cases.

### Single vs Multi DPF

In case a particular deployment uses just one DPF, the entire selection process can be simplified to always return the
pre-configured DPF URL. This is likely to be used in end-to-end-test scenarios, demos or samples.

### Store concepts

The DPF selector needs a storage backend where it can save its `DataPlaneInstance` objects. The default implementation
will be an in-memory one, but extensions can be provided for CosmosDB, SQL, etc.

Whether a centralized persistent or an ephemeral store should be used will largely depend on the deployment scenario and
whether the DPF selector runs standalone or embedded in the connector.

**Seeding:** As a starting point we will provide a mechanism to load a preconfigured list of DPF instances using the EDC
configuration mechanism. Named properties similar will be used, similar to what we currently have for web contexts, for
example:

```properties
edc.dpf.selector.pod1.url=http://some.url
edc.dpf.selector.pod1.someOtherProperty=...
//...=
edc.dpf.selector.pod2.url=http://some.other.url
edc.dpf.selector.pod2.someOtherProperty=...
```

In addition, using the aforementioned API, any sort of data seeding scenario is possible, e.g. terraform, a dedicated
loader, scripting, etc.

### Selection Strategies

A `SelectionStrategy` (as in: [strategy pattern](https://en.wikipedia.org/wiki/Strategy_pattern)) influences the
best-fit criteria for a `DataPlaneInstance`. By default, we will implement the following ones:

- `RandomSelectionStrategy`: will select one DPF instance at random. Assuming normal distribution of the RNG, all DPFs
  will be selected at approximately equal frequency.
- `RoundRobinSelectionStrategy`: selects the "next" DPF instance in an ordered list. **Requires state!**
- [optional] `OldestNextSelectionStrategy`: selects the DPF instance that was idle the longest

_Please note that when multiple DPF selector instances are used, some of the selection strategies only work as intended
if DPF selectors share their storage, otherwise the counters would be inconsistent!_

All `SelectionStrategy` objects must be registered with the DPF selector before it can be passed in through a REST API
or used in the SPI. This requires the use of a `SelectionStrategyRegistry` into which customer extensions can register
their specific strategies.

## Deployment scenarios

With this modular approach, the DPF selector can run as a standalone application, for example in a Kubernetes Deployment
or as plain sidecar container. However, this type of deployment scenario practically _requires_ adding the `-api` module
to the build, otherwise no interaction with the selector is possible.

Another way of running a DPF selector would be as extension, i.e. embedded in an EDC connector runtime. In this scenario
the `-api` module can be ignored, as all communication happens through the Java SPI. Again, situations with multiple
connector instances practically _require_ using a persistent centralized storage (such as CosmosDB).

**Client lib**: it should be transparent for the connector whether an embedded or a remote DPF selector is used,
therefore we propose creating a small "DPF selector lib" in the EDC codebase, consisting of a single interface:

```java
public interface DataPlaneSelectorClient {
    DataPlaneInstance select(DataRequest dataRequest);
}

// when the DPF selector is embedded --> default
class LocalDataPlaneSelectorClient implements DataPlaneSelectorClient {
    private final DataPlaneSelector selector;
    private SelectionStrategy strategy; //could be null

    // CTor(s)

    public DataPlaneInstance select(DataRequest dataRequest) {
        return strategy != null ? selector.select(dataRequest, strategy) : selector.select(dataRequest);
    }
}

// when the DPF selector is remote
class RemoteDataPlaneSelectorClient implements DataPlaneSelectorClient {
    private final EdcHttpClient client;
    private String strategyName; //could be null

    // CTor(s)

    public DataPlaneInstance select(DataRequest dataRequest) {
        //invoke REST API of the remote selector 
    }
}
```

## Future developments

Once we have performance metrics and instrumentation built-in into the DPF, we can create a small extension that reports
those dynamic metrics back to the DPF selector, who then stores them and uses them as additional input parameter to the
selection algorithm.

Another possible improvement would be a DPF selector that can interact with the Kubernetes backend and dynamically
update its internal storage based on Kubernetes auto-scaling: when Kubernetes scales out the DPF pods, the selector
tracks that and automatically updates its internal storage.
