/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.cache.crawler.CatalogCrawler;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerErrorHandler;
import org.eclipse.dataspaceconnector.catalog.spi.CrawlerSuccessHandler;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapterRegistry;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * The execution manager is responsible for instantiating crawlers and delegating the incoming work items among them.
 * Work items are fetched directly from the {@link FederatedCacheNodeDirectory}, crawlers are instantiated before starting the run and will be reused.
 * For example, a list of 10 work items and 2 {@link CatalogCrawler} objects would mean that every crawler gets invoked 5 times.
 * All this is done
 * <p>
 * Pre- and Post-Tasks can be registered to perform preparatory or cleanup operations.
 * <p>
 * The ExecutionManager delegates the actual task to the {@link ExecutionPlan}, which determines, when and how often it needs to be run.
 */
public class ExecutionManager {

    private Monitor monitor;
    private Runnable preExecutionTask;
    private Runnable postExecutionTask;
    private FederatedCacheNodeDirectory directory;
    private String connectorId;
    private int numCrawlers = 1;
    private NodeQueryAdapterRegistry nodeQueryAdapterRegistry;
    private CrawlerSuccessHandler successHandler;

    private ExecutionManager() {
    }

    public void executePlan(ExecutionPlan plan) {
        plan.run(() -> {

            monitor.info(message("Run pre-execution task"));
            runPreExecution();

            monitor.info(message("Run execution"));
            doWork();

            monitor.info(message("Run post-execution task"));
            runPostExecution();
        });

    }

    private void doWork() {
        // load work items from directory
        List<WorkItem> workItems = fetchWorkItems();
        if (workItems.isEmpty()) {
            monitor.warning("No WorkItems found, aborting execution");
            return;
        }
        monitor.debug(message("Loaded " + workItems.size() + " work items from storage"));
        var allItems = new ArrayBlockingQueue<>(workItems.size(), true, workItems);

        monitor.debug(message("Instantiate crawlers..."));
        //instantiate fixed pool of crawlers
        var errorHandler = createErrorHandlers(monitor, allItems);

        var actualNumCrawlers = Math.min(allItems.size(), numCrawlers);
        monitor.debug(format(message("Crawler parallelism is %s, based on config and number of work items"), actualNumCrawlers));
        var availableCrawlers = createCrawlers(errorHandler, actualNumCrawlers);

        while (!allItems.isEmpty()) {
            // try get next available crawler
            var crawler = nextAvailableCrawler(availableCrawlers);
            if (crawler == null) {
                monitor.debug(message("No crawler available, will retry later"));
                continue;
            }

            var item = allItems.poll();
            if (item == null) {
                monitor.warning(message("WorkItem queue empty, abort execution"));
                break;
            }

            // for now use the first adapter that can handle the protocol
            var adapter = nodeQueryAdapterRegistry.findForProtocol(item.getProtocol()).stream().findFirst();
            if (adapter.isEmpty()) {
                monitor.warning(message(format("No protocol adapter found for protocol '%s'", item.getProtocol())));
            } else {
                crawler.run(item, adapter.get())
                        .whenComplete((updateResponse, throwable) -> {
                            if (throwable != null) {
                                monitor.severe(message(format("Unexpected exception happened during in crawler %s", crawler.getId())), throwable);
                            } else {
                                monitor.info(message(format("Crawler [%s] is done", crawler.getId())));
                            }
                            availableCrawlers.add(crawler);
                        });
            }
        }
    }

    @Nullable
    private CatalogCrawler nextAvailableCrawler(ArrayBlockingQueue<CatalogCrawler> availableCrawlers) {
        CatalogCrawler crawler = null;
        try {
            monitor.debug(message("Getting next available crawler"));
            crawler = availableCrawlers.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            monitor.debug("interrupted while waiting for crawler to become available");
        }
        return crawler;
    }

    private void runPostExecution() {
        if (postExecutionTask != null) {
            try {
                postExecutionTask.run();
            } catch (Throwable thr) {
                monitor.severe("Error running post execution task", thr);
            }
        }
    }

    private void runPreExecution() {
        if (preExecutionTask != null) {
            try {
                preExecutionTask.run();
            } catch (Throwable thr) {
                monitor.severe("Error running pre execution task", thr);
            }
        }
    }

    @NotNull
    private ArrayBlockingQueue<CatalogCrawler> createCrawlers(CrawlerErrorHandler errorHandler, int numCrawlers) {
        return new ArrayBlockingQueue<>(numCrawlers, true, IntStream.range(0, numCrawlers).mapToObj(i -> new CatalogCrawler(monitor, errorHandler, successHandler)).collect(Collectors.toList()));
    }

    private List<WorkItem> fetchWorkItems() {
        // use all nodes EXCEPT self
        return directory.getAll().stream().filter(node -> !node.getName().equals(connectorId)).map(n -> new WorkItem(n.getTargetUrl(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());
    }

    private String selectProtocol(List<String> supportedProtocols) {
        //just take the first matching one.
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    @NotNull
    private CrawlerErrorHandler createErrorHandlers(Monitor monitor, Queue<WorkItem> workItems) {
        return workItem -> {
            if (workItem.getErrors().size() > 7) {
                monitor.severe(message(format("The following workitem has errored out more than 5 times. We'll discard it now: [%s]", workItem)));
            } else {
                var random = new Random();
                var to = 5 + random.nextInt(20);
                monitor.debug(message(format("The following work item has errored out. Will re-queue after a small delay: [%s]", workItem)));
                Executors.newSingleThreadScheduledExecutor().schedule(() -> workItems.offer(workItem), to, TimeUnit.SECONDS);
            }
        };
    }

    private String message(String input) {
        return "ExecutionManager: " + input;
    }


    public static final class Builder {

        private final ExecutionManager instance;

        private Builder() {
            instance = new ExecutionManager();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            instance.monitor = monitor;
            return this;
        }

        public Builder preExecutionTask(Runnable preExecutionTask) {
            instance.preExecutionTask = preExecutionTask;
            return this;
        }

        public Builder connectorId(String connectorId) {
            instance.connectorId = connectorId;
            return this;
        }

        public Builder numCrawlers(int numCrawlers) {
            instance.numCrawlers = numCrawlers;
            return this;
        }

        public Builder postExecutionTask(Runnable postExecutionTask) {
            instance.postExecutionTask = postExecutionTask;
            return this;
        }

        public Builder nodeQueryAdapterRegistry(NodeQueryAdapterRegistry registry) {
            instance.nodeQueryAdapterRegistry = registry;
            return this;
        }

        public Builder nodeDirectory(FederatedCacheNodeDirectory directory) {
            instance.directory = directory;
            return this;
        }

        public Builder onSuccess(CrawlerSuccessHandler successConsumer) {
            instance.successHandler = successConsumer;
            return this;
        }

        public ExecutionManager build() {
            Objects.requireNonNull(instance.monitor, "ExecutionManager.Builder: Monitor cannot be null");
            Objects.requireNonNull(instance.connectorId, "ExecutionManager.Builder: connectorId cannot be null");
            Objects.requireNonNull(instance.nodeQueryAdapterRegistry, "ExecutionManager.Builder: nodeQueryAdapterRegistry cannot be null");
            Objects.requireNonNull(instance.directory, "ExecutionManager.Builder: nodeDirectory cannot be null");
            return instance;
        }
    }
}
