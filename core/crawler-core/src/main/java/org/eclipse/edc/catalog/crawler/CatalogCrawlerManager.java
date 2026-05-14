/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler;

import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.model.CatalogUpdateResponse;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.crawler.spi.model.UpdateRequest;
import org.eclipse.edc.crawler.spi.model.UpdateResponse;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Catalog Crawler Manager
 */
public class CatalogCrawlerManager {

    private Monitor monitor;
    private TargetNodeDirectory directory;
    private TargetNodeFilter nodeFilter;
    private CrawlerActionRegistry crawlerActionRegistry;
    private ScheduledExecutorService crawlers;
    private CatalogCrawlerConfiguration configuration;
    private FederatedCatalogCache store;
    private ScheduledExecutorService scheduler;

    private CatalogCrawlerManager() {
        nodeFilter = n -> true;
    }

    public void start() {
        if (!configuration.enabled()) {
            monitor.warning("Catalog Crawler is globally disabled.");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("catalog-crawler-manager");
            return thread;
        });

        scheduler.schedule(this::crawlCatalogs, configuration.delaySeconds(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (!configuration.enabled()) {
            monitor.warning("Catalog Crawler is globally disabled.");
            return;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitor.severe("CatalogCrawlerManager await termination failed", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void crawlCatalogs() {
        store.deleteExpired();
        store.expireAll();

        var workItems = directory.getAll().stream()
                .filter(nodeFilter) // use all nodes EXCEPT self
                .toList();

        if (workItems.isEmpty()) {
            return;
        }

        workItems.stream().map(node -> new TargetNodeRetryCount(0, node))
                .map(this::createCrawler).forEach(crawler -> crawlers.execute(crawler));

        scheduler.schedule(this::crawlCatalogs, configuration.periodSeconds(), TimeUnit.SECONDS);
        monitor.debug("catalog crawling completed: %d entries evaluated. Next execution in %s".formatted(workItems.size(), Duration.ofSeconds(configuration.periodSeconds())));
    }

    private Runnable createCrawler(TargetNodeRetryCount item) {
        return () -> {
            var targetNode = item.targetNode();
            var protocol = selectProtocol(targetNode.supportedProtocols());
            var adapter = crawlerActionRegistry.findForProtocol(protocol).stream().findFirst();
            if (adapter.isEmpty()) {
                monitor.warning(format("No protocol adapter found for protocol '%s'", protocol));
                return;
            }

            var updateRequest = new UpdateRequest(targetNode.id(), targetNode.targetUrl(), protocol);
            adapter.get().apply(updateRequest)
                    .thenAccept(this::persist)
                    .whenComplete((v, throwable) -> onCompletion(item, throwable));
        };
    }

    private void onCompletion(TargetNodeRetryCount item, Throwable throwable) {
        var targetNode = item.targetNode();
        if (throwable == null) {
            monitor.debug(format("Node [%s] is done", targetNode.id()));
        } else {

            if (throwable.getCause() instanceof ConnectException connectException) {
                monitor.info("Cannot connect to node %s: " + connectException.getMessage());
            } else {
                monitor.severe("Unexpected exception occurred while crawling: " + targetNode.id(), throwable);
            }

            var retries = item.retries() + 1;
            if (retries > configuration.maxRetries()) {
                monitor.severe(format("The following Node has errored out more than %d times. We'll discard it now: [%s]", configuration.maxRetries(), item));
            } else {
                monitor.debug(format("The following Node has errored out. Will re-queue after a delay of %s seconds: [%s]", configuration.retryDelaySeconds(), item));
                crawlers.schedule(createCrawler(item), configuration.retryDelaySeconds(), TimeUnit.SECONDS);
            }
        }
    }

    private String selectProtocol(List<String> supportedProtocols) {
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    private void persist(UpdateResponse updateResponse) {
        if (updateResponse instanceof CatalogUpdateResponse catalogUpdateResponse) {
            var catalog = catalogUpdateResponse.getCatalog();
            catalog.getProperties().put(CatalogConstants.PROPERTY_ORIGINATOR, updateResponse.getSource());
            store.save(catalog);
        } else {
            monitor.warning("Expected a response of type %s but got %s. Will discard".formatted(CatalogUpdateResponse.class, updateResponse.getClass()));
        }
    }

    private record TargetNodeRetryCount(int retries, TargetNode targetNode) {

    }

    public static final class Builder {

        private final CatalogCrawlerManager instance;

        private Builder() {
            instance = new CatalogCrawlerManager();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            instance.monitor = monitor;
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

        public Builder configuration(CatalogCrawlerConfiguration catalogCrawlerConfiguration) {
            instance.configuration = catalogCrawlerConfiguration;
            return this;
        }

        public Builder store(FederatedCatalogCache store) {
            instance.store = store;
            return this;
        }

        public CatalogCrawlerManager build() {
            Objects.requireNonNull(instance.configuration, "ExecutionManager.Builder: Configuration cannot be null");
            Objects.requireNonNull(instance.monitor, "ExecutionManager.Builder: Monitor cannot be null");
            Objects.requireNonNull(instance.crawlerActionRegistry, "ExecutionManager.Builder: nodeQueryAdapterRegistry cannot be null");
            Objects.requireNonNull(instance.directory, "ExecutionManager.Builder: nodeDirectory cannot be null");

            instance.crawlers = Executors.newScheduledThreadPool(instance.configuration.numCrawlers());

            return instance;
        }
    }
}
