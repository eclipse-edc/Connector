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

package org.eclipse.edc.jsonld.cache;

import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContextService;
import org.eclipse.edc.jsonld.cache.spi.PullStrategy;
import org.eclipse.edc.jsonld.cache.spi.resolver.JsonLdContextResolver;
import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
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

import static org.eclipse.edc.jsonld.cache.JsonLdContextCacheExtension.NAME;

/**
 * Provides the {@link CachedJsonLdContextService}, synchronizes the persisted cache into the {@link JsonLd}
 * service at boot, and periodically refreshes refreshable entries.
 */
@Extension(NAME)
public class JsonLdContextCacheExtension implements ServiceExtension {

    public static final String NAME = "JSON-LD Context Cache";

    private static final long DEFAULT_REFRESH_INTERVAL_MS = 3600_000L;

    @Setting(description = "Enables the background refresh of cached JSON-LD contexts", key = "edc.jsonld.cache.refresh.enabled", defaultValue = "true")
    private boolean refreshEnabled;

    @Setting(description = "Interval (in milliseconds) between background refreshes of cached JSON-LD contexts with pull strategy 'always'.",
            key = "edc.jsonld.cache.refresh.interval.ms", defaultValue = DEFAULT_REFRESH_INTERVAL_MS + "")
    private long refreshIntervalMs;

    @Inject
    private JsonLd jsonLd;
    @Inject
    private CachedJsonLdContextStore store;
    @Inject
    private JsonLdContextResolver resolver;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private Clock clock;

    private Monitor monitor;
    private CachedJsonLdContextService service;
    private ScheduledExecutorService scheduler;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        service = new CachedJsonLdContextServiceImpl(transactionContext, store, resolver, jsonLd, monitor, clock);
    }

    @Provider
    public CachedJsonLdContextService cachedJsonLdContextService() {
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

    private void register(CachedJsonLdContext context) {
        if (context.getContent() == null) {
            return;
        }
        JsonLdContexts.toJsonObject(context.getContent())
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
                            .map(CachedJsonLdContext::getId)
                            .toList();
                }
            });
            due.forEach(service::refresh);
        } catch (Exception e) {
            monitor.warning("Error while refreshing JSON-LD context cache", e);
        }
    }

    private boolean shouldFetch(CachedJsonLdContext c) {
        return (c.getPullStrategy() == PullStrategy.ALWAYS) || (c.getPullStrategy() == PullStrategy.IF_NOT_PRESENT && c.getContent() == null);
    }
}
