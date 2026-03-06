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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Add shutdown method
 *
 */

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.CrawlerSuccessHandler;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.crawler.spi.WorkItem;
import org.eclipse.edc.crawler.spi.model.ExecutionPlan;
import org.eclipse.edc.crawler.spi.model.UpdateRequest;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * The execution manager is responsible for instantiating crawlers and delegating the incoming work items among them.
 * Work items are fetched directly from the {@link TargetNodeDirectory}, crawlers are instantiated before starting the run and will be reused.
 * <p>
 * Pre- and Post-Tasks can be registered to perform preparatory or cleanup operations.
 * <p>
 * The ExecutionManager delegates the actual task to the {@link ExecutionPlan}, which determines, when and how often it needs to be run.
 */
public class ExecutionManager {

    private Monitor monitor;
    private Runnable preExecutionTask;
    private Runnable postExecutionTask;
    private TargetNodeDirectory directory;
    private TargetNodeFilter nodeFilter;
    private CrawlerActionRegistry crawlerActionRegistry;
    private CrawlerSuccessHandler successHandler;
    private ScheduledExecutorService crawlers;
    private CatalogCrawlerConfiguration configuration;

    private ExecutionManager() {
        nodeFilter = n -> true;
    }

    public void executePlan(ExecutionPlan plan) {
        if (!configuration.enabled()) {
            monitor.warning("Execution of crawlers is globally disabled.");
            return;
        }

        plan.run(() -> {
            runTask("pre-execution", preExecutionTask);
            doWork();
            runTask("post-execution", postExecutionTask);
        });

    }

    public void shutdownPlan(ExecutionPlan plan) {
        if (!configuration.enabled()) {
            monitor.warning("Execution of crawlers is globally disabled.");
            return;
        }
        plan.stop();
    }

    private void doWork() {
        var workItems = fetchWorkItems();
        if (workItems.isEmpty()) {
            return;
        }

        monitor.debug("Loaded " + workItems.size() + " work items from storage");

        workItems.stream().map(this::createCrawler).forEach(crawler -> crawlers.execute(crawler));
    }

    private Runnable createCrawler(WorkItem item) {
        return () -> {
            var adapter = crawlerActionRegistry.findForProtocol(item.getProtocol()).stream().findFirst();
            if (adapter.isEmpty()) {
                monitor.warning(format("No protocol adapter found for protocol '%s'", item.getProtocol()));
            } else {
                var updateRequest = new UpdateRequest(item.getId(), item.getUrl(), item.getProtocol());
                adapter.get().apply(updateRequest)
                        .thenAccept(successHandler)
                        .whenComplete((v, throwable) -> onCompletion(item, throwable));
            }
        };
    }

    private void onCompletion(WorkItem item, Throwable throwable) {
        if (throwable == null) {
            monitor.debug(format("WorkItem [%s] is done", item.getId()));
        } else {
            item.error(throwable.getMessage());
            monitor.severe("Unexpected exception occurred while crawling: " + item.getId(), throwable);
            if (item.getErrors().size() > configuration.maxRetries()) {
                monitor.severe(format("The following WorkItem has errored out more than %d times. We'll discard it now: [%s]", configuration.maxRetries(), item));
            } else {
                monitor.debug(format("The following work item has errored out. Will re-queue after a delay of %s seconds: [%s]", configuration.retryDelaySeconds(), item));
                crawlers.schedule(createCrawler(item), configuration.retryDelaySeconds(), TimeUnit.SECONDS);
            }
        }
    }

    private void runTask(String description, Runnable runnable) {
        if (runnable != null) {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                monitor.severe("Error running %s task".formatted(description), throwable);
            }
        }
    }

    private List<WorkItem> fetchWorkItems() {
        // use all nodes EXCEPT self
        return directory.getAll().stream()
                .filter(nodeFilter)
                .map(n -> new WorkItem(n.id(), n.targetUrl(), selectProtocol(n.supportedProtocols())))
                .collect(Collectors.toList());
    }

    private String selectProtocol(List<String> supportedProtocols) {
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
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

        public Builder postExecutionTask(Runnable postExecutionTask) {
            instance.postExecutionTask = postExecutionTask;
            return this;
        }

        public Builder nodeQueryAdapterRegistry(CrawlerActionRegistry registry) {
            instance.crawlerActionRegistry = registry;
            return this;
        }

        public Builder nodeDirectory(TargetNodeDirectory directory) {
            instance.directory = directory;
            return this;
        }

        public Builder nodeFilterFunction(TargetNodeFilter filter) {
            instance.nodeFilter = filter;
            return this;
        }

        public Builder onSuccess(CrawlerSuccessHandler successConsumer) {
            instance.successHandler = successConsumer;
            return this;
        }

        public Builder configuration(CatalogCrawlerConfiguration catalogCrawlerConfiguration) {
            instance.configuration = catalogCrawlerConfiguration;
            return this;
        }

        public ExecutionManager build() {
            Objects.requireNonNull(instance.configuration, "ExecutionManager.Builder: Configuration cannot be null");
            Objects.requireNonNull(instance.monitor, "ExecutionManager.Builder: Monitor cannot be null");
            Objects.requireNonNull(instance.crawlerActionRegistry, "ExecutionManager.Builder: nodeQueryAdapterRegistry cannot be null");
            Objects.requireNonNull(instance.directory, "ExecutionManager.Builder: nodeDirectory cannot be null");

            instance.crawlers = Executors.newScheduledThreadPool(instance.configuration.numCrawlers());

            return instance;
        }
    }
}
