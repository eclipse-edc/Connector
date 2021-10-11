package org.eclipse.dataspaceconnector.catalog.cache;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.loader.LoaderManagerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.management.PartitionManagerImpl;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.LoaderManager;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class FederatedCatalogCacheExtension implements ServiceExtension {
    private static final int DEFAULT_QUEUE_LENGTH = 50;
    private static final int DEFAULT_BATCH_SIZE = 5;
    private static final int DEFAULT_RETRY_TIMEOUT_MILLIS = 2000;

    @Override
    public Set<String> provides() {
        return Set.of(Crawler.FEATURE, LoaderManager.FEATURE);
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:retry-policy", FederatedCacheNodeDirectory.FEATURE, ProtocolAdapterRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var queue = new ArrayBlockingQueue<UpdateResponse>(DEFAULT_QUEUE_LENGTH);

        // protocol registry
        var protocolAdapterRegistry = context.getService(ProtocolAdapterRegistry.class);

        // get all known nodes
        var directory = context.getService(FederatedCacheNodeDirectory.class);
        List<WorkItem> nodes = directory.getAll().stream().map(n -> new WorkItem(n.getUrl().toString(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());
        // lets create a simple partition manager:
        var partitionManager = new PartitionManagerImpl(context.getMonitor(), new InMemoryWorkItemQueue(50), workItems -> createCrawler(workItems, context, protocolAdapterRegistry), 3, nodes);

        // and a loader manager
        var loaderManager = new LoaderManagerImpl(queue, List.of(batch -> context.getMonitor().info("Storing batch of size " + batch.size())), DEFAULT_BATCH_SIZE, () -> DEFAULT_RETRY_TIMEOUT_MILLIS);


    }

    @Override
    public void start() {
        ServiceExtension.super.start();
    }

    @Override
    public void shutdown() {
        ServiceExtension.super.shutdown();
    }

    private String selectProtocol(List<String> supportedProtocols) {
        //just take the first matching one.
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    private Crawler createCrawler(WorkItemQueue workItems, ServiceExtensionContext context, ProtocolAdapterRegistry protocolAdapters) {
        var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        return CrawlerImpl.Builder.newInstance()
                .monitor(context.getMonitor())
                .retryPolicy(retryPolicy)
                .workItems(workItems)
                .protocolAdapters(protocolAdapters)
                .workQueuePollTimeout(Duration.ofSeconds(5))
                .build();
    }
}
