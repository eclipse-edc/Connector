/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.cache.controller.FederatedCatalogApiController;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CrawlerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.NodeQueryAdapterRegistryImpl;
import org.eclipse.dataspaceconnector.catalog.cache.loader.LoaderManagerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.management.PartitionManagerImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.CacheQueryAdapterImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.CacheQueryAdapterRegistryImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.IdsMultipartNodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryEngineImpl;
import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.LoaderManager;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionConfiguration;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Provides({Crawler.class, LoaderManager.class, QueryEngine.class, NodeQueryAdapterRegistry.class, CacheQueryAdapterRegistry.class})
public class FederatedCatalogCacheExtension implements ServiceExtension {
    public static final int DEFAULT_NUM_CRAWLERS = 1;
    private static final int DEFAULT_QUEUE_LENGTH = 50;
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final int DEFAULT_RETRY_TIMEOUT_MILLIS = 2000;
    private LoaderManager loaderManager;
    private PartitionManager partitionManager;
    private PartitionConfiguration partitionManagerConfig;
    private Monitor monitor;
    private ArrayBlockingQueue<UpdateResponse> updateResponseQueue;
    @Inject
    private FederatedCacheStore store;
    @Inject
    private WebService webService;
    @Inject(required = false)
    private HealthCheckService healthCheckService;
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    // protocol registry - must be supplied by another extension
    // get all known nodes from node directory - must be supplied by another extension
    @Inject
    private FederatedCacheNodeDirectory directory;
    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // QUERY SUBSYSTEM
        var queryAdapterRegistry = new CacheQueryAdapterRegistryImpl();
        context.registerService(CacheQueryAdapterRegistry.class, queryAdapterRegistry);

        queryAdapterRegistry.register(new CacheQueryAdapterImpl(store));
        var queryEngine = new QueryEngineImpl(queryAdapterRegistry);
        context.registerService(QueryEngine.class, queryEngine);
        monitor = context.getMonitor();
        var catalogController = new FederatedCatalogApiController(monitor, queryEngine);
        webService.registerResource(catalogController);

        // contribute to the liveness probe
        if (healthCheckService != null) {
            healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("FCC Query API").build());
        }

        // CRAWLER SUBSYSTEM
        var nodeQueryAdapterRegistry = new NodeQueryAdapterRegistryImpl();

        // catalog queries via IDS multipart are supported by default
        nodeQueryAdapterRegistry.register("ids-multipart", new IdsMultipartNodeQueryAdapter(context.getConnectorId(), dispatcherRegistry));
        context.registerService(NodeQueryAdapterRegistry.class, nodeQueryAdapterRegistry);

        updateResponseQueue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_LENGTH);
        //todo: maybe get this from a database or somewhere else?
        partitionManagerConfig = new PartitionConfiguration(context);
        // lets create a simple partition manager
        partitionManager = createPartitionManager(context, updateResponseQueue, nodeQueryAdapterRegistry);
        // and a loader manager
        loaderManager = createLoaderManager(store);

        monitor.info("Federated Catalog Cache extension initialized");
    }

    @Override
    public void start() {
        partitionManager.schedule(partitionManagerConfig.getExecutionPlan());
        loaderManager.start(updateResponseQueue);
        monitor.info("Federated Catalog Cache extension started");
    }

    @Override
    public void shutdown() {
        partitionManager.stop();
        loaderManager.stop();
        monitor.info("Federated Catalog Cache extension stopped");
    }

    @NotNull
    private LoaderManager createLoaderManager(FederatedCacheStore store) {
        return LoaderManagerImpl.Builder.newInstance()
                .loaders(List.of(new LoaderImpl(store)))
                .batchSize(partitionManagerConfig.getLoaderBatchSize(DEFAULT_BATCH_SIZE))
                .waitStrategy(() -> partitionManagerConfig.getLoaderRetryTimeout(DEFAULT_RETRY_TIMEOUT_MILLIS))
                .monitor(monitor)
                .build();
    }

    @NotNull
    private PartitionManager createPartitionManager(ServiceExtensionContext context, ArrayBlockingQueue<UpdateResponse> updateResponseQueue, NodeQueryAdapterRegistry protocolAdapterRegistry) {

        // use all nodes EXCEPT self
        Supplier<List<WorkItem>> nodes = () -> directory.getAll().stream()
                .filter(node -> !node.getName().equals(context.getConnectorId()))
                .map(n -> new WorkItem(n.getTargetUrl(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());

        return new PartitionManagerImpl(monitor,
                new DefaultWorkItemQueue(partitionManagerConfig.getWorkItemQueueSize(10)),
                workItems -> createCrawler(workItems, context, protocolAdapterRegistry, updateResponseQueue),
                partitionManagerConfig.getNumCrawlers(DEFAULT_NUM_CRAWLERS),
                nodes);
    }


    private String selectProtocol(List<String> supportedProtocols) {
        //just take the first matching one.
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    private Crawler createCrawler(WorkItemQueue workItems, ServiceExtensionContext context, NodeQueryAdapterRegistry protocolAdapters, ArrayBlockingQueue<UpdateResponse> updateQueue) {
        return CrawlerImpl.Builder.newInstance()
                .monitor(context.getMonitor())
                .retryPolicy(retryPolicy)
                .workItems(workItems)
                .queue(updateQueue)
                .errorReceiver(getErrorWorkItemConsumer(context, workItems))
                .protocolAdapters(protocolAdapters)
                .workQueuePollTimeout(() -> Duration.ofMillis(2000 + ThreadLocalRandom.current().nextInt(3000)))
                .build();
    }

    @NotNull
    private CrawlerErrorHandler getErrorWorkItemConsumer(ServiceExtensionContext context, WorkItemQueue workItems) {
        return workItem -> {
            if (workItem.getErrors().size() > 7) {
                context.getMonitor().severe(format("The following workitem has errored out more than 5 times. We'll discard it now: [%s]", workItem));
            } else {
                var random = new Random();
                var to = 5 + random.nextInt(20);
                context.getMonitor().info(format("The following work item has errored out. Will re-queue after a small delay: [%s]", workItem));
                Executors.newSingleThreadScheduledExecutor().schedule(() -> workItems.offer(workItem), to, TimeUnit.SECONDS);
            }
        };
    }
}
