package org.eclipse.dataspaceconnector.catalog.cache;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.controller.CatalogController;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.loader.LoaderManagerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.management.PartitionManagerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryEngineImpl;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.LoaderManager;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionConfiguration;
import org.eclipse.dataspaceconnector.catalog.spi.ProtocolAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
        return Set.of("edc:retry-policy", FederatedCacheNodeDirectory.FEATURE, ProtocolAdapterRegistry.FEATURE, QueryAdapterRegistry.FEATURE, "edc:webservice");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // QUERY SUBSYSTEM
        var queryAdapterRegistry = context.getService(QueryAdapterRegistry.class);
        var webService = context.getService(WebService.class);
        var queryEngine = new QueryEngineImpl(queryAdapterRegistry);
        var catalogController = new CatalogController(context.getMonitor(), queryEngine);
        webService.registerController(catalogController);

        // CRAWLER SUBSYSTEM
        var queue = new ArrayBlockingQueue<UpdateResponse>(DEFAULT_QUEUE_LENGTH);

        // protocol registry - must be supplied by another extension
        var protocolAdapterRegistry = context.getService(ProtocolAdapterRegistry.class);

        // get all known nodes from node directory - must be supplied by another extension
        var directory = context.getService(FederatedCacheNodeDirectory.class);
        List<WorkItem> nodes = directory.getAll().stream().map(n -> new WorkItem(n.getUrl().toString(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());

        //todo: maybe get this from a database or somewhere else?
        var partitionConfig = new PartitionConfiguration(context);

        // lets create a simple partition manager
        var partitionManager = new PartitionManagerImpl(context.getMonitor(),
                new InMemoryWorkItemQueue(partitionConfig.getWorkItemQueueSize(10)),
                workItems -> createCrawler(workItems, context, protocolAdapterRegistry),
                partitionConfig.getNumCrawlers(1),
                nodes);

        // and a loader manager
        var loaderManager = new LoaderManagerImpl(queue,
                List.of(batch -> context.getMonitor().info("Storing batch of size " + batch.size())),
                partitionConfig.getLoaderBatchSize(DEFAULT_BATCH_SIZE),
                () -> partitionConfig.getLoaderRetryTimeout(DEFAULT_RETRY_TIMEOUT_MILLIS));

        partitionManager.schedule(partitionConfig.getExecutionPlan());
        loaderManager.start();
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
                .errorReceiver(getErrorWorkItemConsumer(context, workItems))
                .protocolAdapters(protocolAdapters)
                .workQueuePollTimeout(() -> Duration.ofMillis(2000 + new Random().nextInt(3000)))
                .build();
    }

    @NotNull
    private CrawlerErrorHandler getErrorWorkItemConsumer(ServiceExtensionContext context, WorkItemQueue workItems) {
        return workItem -> {
            if (workItem.getErrors().size() > 5) {
                context.getMonitor().severe(format("The following workitem has errored out more than 5 times. We'll discard it now: [%s]", workItem));
            } else {
                context.getMonitor().info("The following work item has errored out. will re-queue after a small delay");
                Executors.newSingleThreadScheduledExecutor().schedule(() -> workItems.offer(workItem), 5, TimeUnit.SECONDS);
            }
        };
    }
}
