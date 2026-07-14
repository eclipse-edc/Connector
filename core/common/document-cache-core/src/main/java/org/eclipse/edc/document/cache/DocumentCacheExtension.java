/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache;

import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentService;
import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.document.cache.spi.resolver.DocumentResolver;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.document.cache.DocumentCacheExtension.NAME;

/**
 * Provides the {@link CachedDocumentService}, synchronizes the persisted cache into the {@link JsonLd}
 * service at boot, and periodically refreshes refreshable entries.
 */
@Extension(NAME)
public class DocumentCacheExtension implements ServiceExtension {

    public static final String NAME = "Document Cache";

    private static final long DEFAULT_REFRESH_INTERVAL_MS = 3600_000L;

    @Setting(description = "Enables the background refresh of cached documents", key = "edc.document.cache.refresh.enabled", defaultValue = "true")
    private boolean refreshEnabled;

    @Setting(description = "Interval (in milliseconds) between background refreshes of cached documents with pull strategy 'always'.",
            key = "edc.document.cache.refresh.interval.ms", defaultValue = DEFAULT_REFRESH_INTERVAL_MS + "")
    private long refreshIntervalMs;

    @Inject
    private JsonLd jsonLd;
    @Inject
    private CachedDocumentStore store;
    @Inject
    private DocumentResolver resolver;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private Clock clock;

    private Monitor monitor;
    private CachedDocumentService service;
    private ScheduledExecutorService scheduler;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        service = new CachedDocumentServiceImpl(transactionContext, store, resolver, jsonLd, monitor, clock);
    }

    @Provider
    public CachedDocumentService cachedDocumentService() {
        return service;
    }

    @Override
    public void start() {
        syncAll();
        if (refreshEnabled) {
            scheduler = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), NAME);
            scheduler.scheduleWithFixedDelay(this::refreshAlways, refreshIntervalMs, refreshIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void syncAll() {
        transactionContext.execute(() -> {
            try (var stream = store.findAll(QuerySpec.max())) {
                stream.forEach(this::register);
            }
        });
    }

    private void register(CachedDocument context) {
        if (context.getType() != CachedDocumentType.JSON_LD || context.getContent() == null) {
            return;
        }
        CachedDocuments.toJsonObject(context.getContent())
                .onSuccess(json -> jsonLd.registerCachedDocument(context.getUrl(), json))
                .onFailure(f -> monitor.warning("Cannot register cached JSON-LD context '%s': %s"
                        .formatted(context.getUrl(), f.getFailureDetail())));
    }

    private void refreshAlways() {
        try {
            var due = transactionContext.execute(() -> {
                try (var stream = store.findAll(QuerySpec.max())) {
                    return stream
                            .filter(this::shouldFetch)
                            .map(CachedDocument::getId)
                            .toList();
                }
            });
            due.forEach(service::refresh);
        } catch (Exception e) {
            monitor.warning("Error while refreshing JSON-LD context cache", e);
        }
    }

    private boolean shouldFetch(CachedDocument c) {
        return (c.getPullStrategy() == PullStrategy.ALWAYS) || (c.getPullStrategy() == PullStrategy.IF_NOT_PRESENT && c.getContent() == null);
    }
}
