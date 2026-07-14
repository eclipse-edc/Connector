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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.document.cache.CachedDocuments.toJsonObject;
import static org.eclipse.edc.document.cache.spi.store.CachedDocumentStore.NOT_FOUND;

public class CachedDocumentServiceImpl implements CachedDocumentService {

    private final TransactionContext transactionContext;
    private final CachedDocumentStore store;
    private final DocumentResolver resolver;
    private final JsonLd jsonLd;
    private final Monitor monitor;
    private final Clock clock;

    public CachedDocumentServiceImpl(TransactionContext transactionContext, CachedDocumentStore store,
                                          DocumentResolver resolver, JsonLd jsonLd, Monitor monitor, Clock clock) {
        this.transactionContext = transactionContext;
        this.store = store;
        this.resolver = resolver;
        this.jsonLd = jsonLd;
        this.monitor = monitor;
        this.clock = clock;
    }

    @Override
    public CachedDocument findById(String id) {
        return transactionContext.execute(() -> store.findById(id));
    }

    @Override
    public ServiceResult<List<CachedDocument>> search(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.findAll(query)) {
                return ServiceResult.success(stream.toList());
            }
        });
    }

    @Override
    public @NotNull ServiceResult<CachedDocument> create(CachedDocument context) {
        return transactionContext.execute(() -> prepare(context)
                .compose(prepared -> ServiceResult.from(store.create(prepared)).onSuccess(this::register)));
    }

    @Override
    public @NotNull ServiceResult<CachedDocument> update(CachedDocument context) {
        return transactionContext.execute(() -> prepare(context)
                .compose(prepared -> ServiceResult.from(store.update(prepared)).onSuccess(this::register)));
    }

    @Override
    public @NotNull ServiceResult<CachedDocument> deleteById(String id) {
        return transactionContext.execute(() -> ServiceResult.from(store.delete(id))
                .onSuccess(deleted -> {
                    if (deleted.getType() == CachedDocumentType.JSON_LD) {
                        jsonLd.unregisterCachedDocument(deleted.getUrl());
                    }
                }));
    }

    @Override
    public @NotNull ServiceResult<CachedDocument> refresh(String id) {
        return transactionContext.execute(() -> {
            var existing = store.findById(id);
            if (existing == null) {
                return ServiceResult.notFound(format(NOT_FOUND, id));
            }
            if (existing.getPullStrategy() == PullStrategy.NEVER) {
                register(existing);
                return ServiceResult.success(existing);
            }
            return pull(existing.getUrl())
                    .compose(content -> {
                        var refreshed = existing.toBuilder()
                                .content(content)
                                .updatedAt(clock.millis())
                                .build();
                        return ServiceResult.from(store.update(refreshed)).onSuccess(this::register);
                    });
        });
    }

    /**
     * Applies the {@link PullStrategy} to obtain the content that will be persisted:
     * {@code NEVER} requires content up front, {@code IF_NOT_PRESENT} pulls only when content is missing,
     * and {@code ALWAYS} pulls a fresh copy from the url.
     */
    private ServiceResult<CachedDocument> prepare(CachedDocument context) {
        return switch (context.getPullStrategy()) {
            case NEVER -> context.getContent() != null
                    ? ServiceResult.success(context)
                    : ServiceResult.badRequest("content is required for pull strategy 'never'");
            case IF_NOT_PRESENT -> context.getContent() != null
                    ? ServiceResult.success(context)
                    : pull(context.getUrl()).map(content -> context.toBuilder().content(content).build());
            case ALWAYS -> pull(context.getUrl()).map(content -> context.toBuilder().content(content).build());
        };
    }

    private ServiceResult<String> pull(String url) {
        var result = resolver.resolve(url);
        if (result.failed()) {
            return ServiceResult.badRequest(result.getFailureDetail());
        }
        return ServiceResult.success(result.getContent().toString());
    }

    /**
     * Registers the document so it can be resolved at runtime. Only {@link CachedDocumentType#JSON_LD} documents
     * are registered into the {@link JsonLd} service; {@link CachedDocumentType#JSON_SCHEMA} documents are served
     * to the management API schema validator directly from the store and therefore need no action here.
     */
    private void register(CachedDocument context) {
        if (context.getType() != CachedDocumentType.JSON_LD) {
            return;
        }
        toJsonObject(context.getContent())
                .onSuccess(json -> jsonLd.registerCachedDocument(context.getUrl(), json))
                .onFailure(f -> monitor.warning("Cannot register cached JSON-LD context '%s': %s"
                        .formatted(context.getUrl(), f.getFailureDetail())));
    }
}
