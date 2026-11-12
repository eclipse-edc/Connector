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

package org.eclipse.edc.catalog.crawler.logic;

import org.eclipse.edc.catalog.crawler.cache.query.DspCatalogFetcher;
import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.crawler.spi.event.TargetNodeCrawled;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration.CATALOG_CRAWLER_ENABLED_SETTING;

/**
 * Catalog Crawler Manager
 */
public class CatalogCrawlerManager {

    private Monitor monitor;
    private TargetNodeDirectory directory;
    private TargetNodeFilter nodeFilter;
    private DspCatalogFetcher catalogFetcher;
    private ScheduledExecutorService crawlers;
    private CatalogCrawlerConfiguration configuration;
    private FederatedCatalogCache store;
    private ParticipantContextStore participantContextStore;
    private EventRouter eventRouter;
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
        var workItems = activeParticipantContextIds().stream()
                .flatMap(participantContextId -> {
                    store.deleteExpired(participantContextId);
                    store.expireAll(participantContextId);
                    return directory.getAll(participantContextId).stream();
                })
                .filter(nodeFilter) // use all nodes EXCEPT self
                .map(node -> new TargetNodeRetryCount(0, node))
                .toList();

        if (workItems.isEmpty()) {
            monitor.debug("No node to crawl, to disable the crawler, please configure the '%s' setting accordingly. Next execution in %s"
                    .formatted(CATALOG_CRAWLER_ENABLED_SETTING, Duration.ofSeconds(configuration.periodSeconds())));
        } else {
            // note: crawling each node emits a single event, because crawling is done in parallel
            workItems.stream().map(this::createCrawler).forEach(crawler -> crawlers.execute(crawler));

            monitor.debug("catalog crawling completed: %d entries evaluated. Next execution in %s"
                    .formatted(workItems.size(), Duration.ofSeconds(configuration.periodSeconds())));
        }

        scheduler.schedule(this::crawlCatalogs, configuration.periodSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Returns the ids of all participant contexts that are not {@link ParticipantContextState#DEACTIVATED}, so the
     * crawler crawls on behalf of each one using its own identity. Deactivated contexts are skipped because their
     * identity is disabled. In classic/single-participant runtimes this returns the single registered participant
     * context.
     */
    private List<String> activeParticipantContextIds() {
        var result = participantContextStore.query(QuerySpec.max());
        if (result.failed()) {
            monitor.warning("Could not fetch participant contexts for crawling: " + result.getFailureDetail());
            return List.of();
        }
        return result.getContent().stream()
                .filter(participantContext -> participantContext.getStateAsEnum() != ParticipantContextState.DEACTIVATED)
                .map(ParticipantContext::getParticipantContextId)
                .toList();
    }

    private Runnable createCrawler(TargetNodeRetryCount item) {
        return () -> {
            var targetNode = item.targetNode();
            var protocol = targetNode.supportedProtocols().stream().findFirst().orElse(null);
            if (protocol == null) {
                monitor.warning(format("Node [%s] does not declare any supported protocol, skipping", targetNode.id()));
                return;
            }

            catalogFetcher.fetch(targetNode.participantContextId(), targetNode.counterPartyId(), targetNode.targetUrl(), protocol)
                    .thenAccept(catalog -> persist(targetNode.participantContextId(), targetNode.targetUrl(), catalog))
                    .whenComplete((v, throwable) -> onCompletion(item, throwable));
        };
    }

    private void onCompletion(TargetNodeRetryCount item, Throwable throwable) {
        var targetNode = item.targetNode();
        if (throwable == null) {
            monitor.debug(format("Node [%s] is done", targetNode.id()));
            publishCrawled(targetNode, null);
        } else {

            if (throwable.getCause() instanceof ConnectException connectException) {
                monitor.info("Cannot connect to node %s: " + connectException.getMessage());
            } else {
                monitor.severe("Unexpected exception occurred while crawling: " + targetNode.id(), throwable);
            }

            var retries = item.retries() + 1;
            if (retries > configuration.maxRetries()) {
                monitor.severe(format("The following Node has errored out more than %d times. We'll discard it now: [%s]", configuration.maxRetries(), item));
                // the failure is only notified once it is terminal, i.e. there are no retries left
                publishCrawled(targetNode, throwable);
            } else {
                monitor.debug(format("The following Node has errored out. Will re-queue after a delay of %s seconds: [%s]", configuration.retryDelaySeconds(), item));
                crawlers.schedule(createCrawler(new TargetNodeRetryCount(retries, targetNode)), configuration.retryDelaySeconds(), TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Notifies subscribers that the node has been crawled, a non-null throwable meaning that crawling it failed.
     */
    private void publishCrawled(TargetNode targetNode, Throwable throwable) {
        var event = TargetNodeCrawled.Builder.newInstance()
                .nodeId(targetNode.id())
                .success(throwable == null);

        if (throwable != null) {
            var cause = throwable;
            if (throwable instanceof CompletionException && throwable.getCause() != null) {
                cause = throwable.getCause();
            }
            event.errorMessage(cause.getMessage() != null ? cause.getMessage() : "Crawling the node failed with an unknown error");
        }

        eventRouter.publish(event.build());
    }

    private void persist(String participantContextId, String source, Catalog catalog) {
        catalog.getProperties().put(CatalogConstants.PROPERTY_ORIGINATOR, source);
        store.save(participantContextId, catalog);
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

        public Builder catalogFetcher(DspCatalogFetcher catalogFetcher) {
            instance.catalogFetcher = catalogFetcher;
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

        public Builder participantContextStore(ParticipantContextStore participantContextStore) {
            instance.participantContextStore = participantContextStore;
            return this;
        }

        public Builder eventRouter(EventRouter eventRouter) {
            instance.eventRouter = eventRouter;
            return this;
        }

        public CatalogCrawlerManager build() {
            Objects.requireNonNull(instance.configuration, "CatalogCrawlerManager.Builder: Configuration cannot be null");
            Objects.requireNonNull(instance.monitor, "CatalogCrawlerManager.Builder: Monitor cannot be null");
            Objects.requireNonNull(instance.catalogFetcher, "CatalogCrawlerManager.Builder: catalogFetcher cannot be null");
            Objects.requireNonNull(instance.directory, "CatalogCrawlerManager.Builder: nodeDirectory cannot be null");
            Objects.requireNonNull(instance.participantContextStore, "CatalogCrawlerManager.Builder: participantContextStore cannot be null");
            Objects.requireNonNull(instance.eventRouter, "CatalogCrawlerManager.Builder: eventRouter cannot be null");

            instance.crawlers = Executors.newScheduledThreadPool(instance.configuration.numCrawlers());

            return instance;
        }
    }
}
