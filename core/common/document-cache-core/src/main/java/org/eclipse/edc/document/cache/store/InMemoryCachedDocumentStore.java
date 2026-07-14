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

package org.eclipse.edc.document.cache.store;

import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * In-memory, threadsafe {@link CachedDocumentStore}. Intended as the default implementation.
 */
public class InMemoryCachedDocumentStore implements CachedDocumentStore {

    private final Map<String, CachedDocument> byId = new ConcurrentHashMap<>();
    private final QueryResolver<CachedDocument> queryResolver;

    public InMemoryCachedDocumentStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(CachedDocument.class, criterionOperatorRegistry);
    }

    @Override
    public CachedDocument findById(String id) {
        return byId.get(id);
    }

    @Override
    public CachedDocument findByUrl(String url) {
        return byId.values().stream().filter(c -> c.getUrl().equals(url)).findFirst().orElse(null);
    }

    @Override
    public Stream<CachedDocument> findAll(QuerySpec spec) {
        return queryResolver.query(byId.values().stream(), spec);
    }

    @Override
    public StoreResult<CachedDocument> create(CachedDocument context) {
        var existingByUrl = findByUrl(context.getUrl());
        if (existingByUrl != null) {
            return StoreResult.alreadyExists(format(URL_ALREADY_EXISTS, context.getUrl()));
        }
        var prev = byId.putIfAbsent(context.getId(), context);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<CachedDocument>alreadyExists(format(ALREADY_EXISTS, context.getId())))
                .orElse(StoreResult.success(context));
    }

    @Override
    public StoreResult<CachedDocument> update(CachedDocument context) {
        var prev = byId.replace(context.getId(), context);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.success(context))
                .orElse(StoreResult.notFound(format(NOT_FOUND, context.getId())));
    }

    @Override
    public StoreResult<CachedDocument> delete(String id) {
        var prev = byId.remove(id);
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(NOT_FOUND, id)));
    }
}
