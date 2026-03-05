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

package org.eclipse.edc.catalog.store;

import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

/**
 * An ephemeral in-memory cache store.
 */
public class InMemoryFederatedCatalogCache implements FederatedCatalogCache {

    private final Map<String, MarkableEntry<Catalog>> cache = new ConcurrentHashMap<>();
    private final LockManager lockManager;
    private final QueryResolver<Catalog> queryResolver;


    public InMemoryFederatedCatalogCache(LockManager lockManager, CriterionOperatorRegistry criterionOperatorRegistry) {
        this.lockManager = lockManager;
        queryResolver = new ReflectionBasedQueryResolver<>(Catalog.class, criterionOperatorRegistry);
    }

    @Override
    public void save(Catalog catalog) {
        lockManager.writeLock(() -> {
            var id = ofNullable(catalog.getProperties().get(CatalogConstants.PROPERTY_ORIGINATOR))
                    .map(Object::toString)
                    .orElse(catalog.getId());
            return cache.put(id, new MarkableEntry<>(false, catalog));
        });
    }

    @Override
    public Collection<Catalog> query(QuerySpec query) {
        var catalogs = cache.values().stream().map(me -> me.entry);
        return lockManager.readLock(() -> queryResolver.query(catalogs, query)).toList();
    }

    @Override
    public void deleteExpired() {
        lockManager.writeLock(() -> {
            cache.values().removeIf(MarkableEntry::isMarked);
            return null;
        });
    }

    @Override
    public void expireAll() {
        cache.replaceAll((k, v) -> new MarkableEntry<>(true, v.getEntry()));
    }

    private static class MarkableEntry<B> {
        private final B entry;
        private final boolean mark;

        MarkableEntry(boolean isMarked, B catalog) {
            entry = catalog;
            mark = isMarked;
        }

        public boolean isMarked() {
            return mark;
        }

        public B getEntry() {
            return entry;
        }

    }
}
