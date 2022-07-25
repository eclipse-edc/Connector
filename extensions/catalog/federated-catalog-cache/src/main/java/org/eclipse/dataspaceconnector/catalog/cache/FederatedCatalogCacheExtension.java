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

import org.eclipse.dataspaceconnector.catalog.cache.controller.FederatedCatalogApiController;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CatalogCrawler;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.NodeQueryAdapterRegistryImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.CacheQueryAdapterImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.CacheQueryAdapterRegistryImpl;
import org.eclipse.dataspaceconnector.catalog.cache.query.IdsMultipartNodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.cache.query.QueryEngineImpl;
import org.eclipse.dataspaceconnector.catalog.directory.InMemoryNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.CacheConfiguration;
import org.eclipse.dataspaceconnector.catalog.spi.CacheQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.QueryEngine;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.store.InMemoryFederatedCacheStore;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

@Provides({ QueryEngine.class, NodeQueryAdapterRegistry.class, CacheQueryAdapterRegistry.class })
public class FederatedCatalogCacheExtension implements ServiceExtension {
    public static final int DEFAULT_NUM_CRAWLERS = 1;
    private static final int DEFAULT_QUEUE_LENGTH = 50;
    private Monitor monitor;
    @Inject
    private FederatedCacheStore store;
    @Inject
    private WebService webService;
    @Inject(required = false)
    private HealthCheckService healthCheckService;
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    // get all known nodes from node directory - must be supplied by another extension
    @Inject
    private FederatedCacheNodeDirectory directory;
    private ExecutionPlan executionPlan;
    private Supplier<List<WorkItem>> workItemSupplier;
    private NodeQueryAdapterRegistryImpl nodeQueryAdapterRegistry;
    private int numCrawlers;

    @Override
    public String name() {
        return "Federated Catalog Cache";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // QUERY SUBSYSTEM
        var queryAdapterRegistry = new CacheQueryAdapterRegistryImpl();
        context.registerService(CacheQueryAdapterRegistry.class, queryAdapterRegistry);

        queryAdapterRegistry.register(new CacheQueryAdapterImpl(store));
        var queryEngine = new QueryEngineImpl(queryAdapterRegistry);
        context.registerService(QueryEngine.class, queryEngine);
        monitor = context.getMonitor();
        var catalogController = new FederatedCatalogApiController(queryEngine);
        webService.registerResource(catalogController);

        // contribute to the liveness probe
        if (healthCheckService != null) {
            healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("FCC Query API").build());
        }

        // CRAWLER SUBSYSTEM

        workItemSupplier = workItemSupplier(context);

        //todo: maybe get this from a database or somewhere else?
        var cacheConfiguration = new CacheConfiguration(context);
        numCrawlers = cacheConfiguration.getNumCrawlers(DEFAULT_NUM_CRAWLERS);
        // and a loader manager

        executionPlan = cacheConfiguration.getExecutionPlan();
    }

    @Override
    public void start() {

        executionPlan.run(() -> {
            store.deleteExpired(); // delete all expired entries before re-populating
            store.expireAll(); // mark all entries as expired, unless they get updated by the crawlers

            // load work items from directory
            List<WorkItem> workItems = workItemSupplier.get();
            var allItems = new ArrayBlockingQueue<>(workItems.size(), true, workItems);

            if (allItems.isEmpty()) {
                monitor.warning("No WorkItems found, aborting execution");
                return;
            }

            //instantiate fixed pool of crawlers
            var errorHandler = createErrorHandlers(monitor, allItems);
            monitor.debug(format("Crawler parallelism is %s, according to config", numCrawlers));

            var availableCrawlers = new ArrayBlockingQueue<>(numCrawlers, true, IntStream.range(0, numCrawlers)
                    .mapToObj(i -> new CatalogCrawler(monitor, store, errorHandler))
                    .collect(Collectors.toList()));

            while (!allItems.isEmpty()) {
                CatalogCrawler crawler = null;
                try {
                    monitor.debug("Waiting for crawler to become available");
                    crawler = availableCrawlers.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    monitor.debug("interrupted while waiting for crawler to become available");
                }
                if (crawler == null) {
                    monitor.debug("No crawler available, will retry");
                    continue;
                }

                var item = allItems.poll();
                if (item == null) {
                    monitor.warning("WorkItem queue empty, abort execution");
                    break;
                }

                var adapters = createNodeQueryAdapterRegistry(null).findForProtocol(item.getProtocol());
                CatalogCrawler activeCrawler = crawler;
                crawler.run(item, adapters).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        monitor.severe(format("Unexpected exception happened during in crawler %s", activeCrawler.getId()), throwable);
                    } else {
                        monitor.info(format("Crawler [%s] is done", activeCrawler.getId()));
                    }
                    availableCrawlers.add(activeCrawler);
                });
            }

        });
    }

    @Override
    public void shutdown() {
        //todo: interrupt execution
    }

    @Provider
    public NodeQueryAdapterRegistry createNodeQueryAdapterRegistry(ServiceExtensionContext context) {

        if (nodeQueryAdapterRegistry == null) {
            nodeQueryAdapterRegistry = new NodeQueryAdapterRegistryImpl();

            // catalog queries via IDS multipart are supported by default
            nodeQueryAdapterRegistry.register("ids-multipart", new IdsMultipartNodeQueryAdapter(context.getConnectorId(), dispatcherRegistry, monitor));
            context.registerService(NodeQueryAdapterRegistry.class, nodeQueryAdapterRegistry);
        }
        return nodeQueryAdapterRegistry;
    }

    @Provider(isDefault = true)
    public FederatedCacheStore defaultCacheStore() {
        //todo: converts every criterion into a predicate that is always true. must be changed later!
        return new InMemoryFederatedCacheStore(criterion -> offer -> true, new LockManager(new ReentrantReadWriteLock()));
    }

    @Provider(isDefault = true)
    public FederatedCacheNodeDirectory defaultNodeDirectory() {
        return new InMemoryNodeDirectory();
    }


    private Supplier<List<WorkItem>> workItemSupplier(ServiceExtensionContext context) {
        // use all nodes EXCEPT self
        return () -> directory.getAll().stream()
                .filter(node -> !node.getName().equals(context.getConnectorId()))
                .map(n -> new WorkItem(n.getTargetUrl(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());
    }


    private String selectProtocol(List<String> supportedProtocols) {
        //just take the first matching one.
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    @NotNull
    private CrawlerErrorHandler createErrorHandlers(Monitor monitor, Queue<WorkItem> workItems) {
        return workItem -> {
            if (workItem.getErrors().size() > 7) {
                monitor.severe(format("The following workitem has errored out more than 5 times. We'll discard it now: [%s]", workItem));
            } else {
                var random = new Random();
                var to = 5 + random.nextInt(20);
                monitor.debug(format("The following work item has errored out. Will re-queue after a small delay: [%s]", workItem));
                Executors.newSingleThreadScheduledExecutor().schedule(() -> workItems.offer(workItem), to, TimeUnit.SECONDS);
            }
        };
    }
}
