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

package org.eclipse.edc.connector.defaults.storage;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.util.concurrency.LockManager;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe entity store for a {@link StatefulEntity}. This implementation is intended for testing
 * purposes only.
 */
public class InMemoryStatefulEntityStore<T extends StatefulEntity<T>> {
    private static final long DEFAULT_LEASE_TIME_MILLIS = 60_000;
    private final Map<String, Item<T>> entitiesById = new ConcurrentHashMap<>();
    private final QueryResolver<T> queryResolver;
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final String lockId;
    private final Clock clock;
    private final Map<String, Lease> leases;

    public InMemoryStatefulEntityStore(Class<T> clazz, String lockId, Clock clock, Map<String, Lease> leases) {
        queryResolver = new ReflectionBasedQueryResolver<>(clazz);
        this.lockId = lockId;
        this.clock = clock;
        this.leases = leases;
    }

    public T find(String id) {
        var t = entitiesById.get(id);
        if (t == null) {
            return null;
        }
        return t.item.copy();
    }

    public void upsert(T entity) {
        acquireLease(entity.getId(), lockId);
        entitiesById.put(entity.getId(), new Item<>(entity.copy()));
        freeLease(entity.getId());
    }

    public void delete(String id) {
        if (isLeased(id)) {
            throw new IllegalStateException("ContractNegotiation is leased and cannot be deleted!");
        }
        entitiesById.remove(id);
    }

    public Stream<T> findAll(QuerySpec querySpec) {
        return queryResolver.query(findAll(), querySpec);
    }

    public @NotNull List<T> nextForState(int state, int max) {
        return lockManager.writeLock(() -> {
            var items = entitiesById.values().stream()
                    .filter(e -> e.item.getState() == state)
                    .filter(e -> !isLeased(e.item.getId()))
                    .sorted(Comparator.comparingLong(e -> e.item.getStateTimestamp())) //order by state timestamp, oldest first
                    .limit(max)
                    .collect(toList());
            items.forEach(i -> acquireLease(i.item.getId(), lockId));
            return items.stream().map(e -> e.item.copy()).collect(toList());
        });
    }

    public Stream<T> findAll() {
        return entitiesById.values().stream().map(e -> e.item);
    }

    private void freeLease(String id) {
        leases.remove(id);
    }

    private void acquireLease(String id, String lockId) {
        if (!isLeased(id) || isLeasedBy(id, lockId)) {
            leases.put(id, new Lease(lockId, clock.millis(), DEFAULT_LEASE_TIME_MILLIS));
        } else {
            throw new IllegalStateException("Cannot acquire lease, is already leased by someone else!");
        }
    }

    private boolean isLeased(String id) {
        return leases.containsKey(id) && !leases.get(id).isExpired(clock.millis());
    }

    private boolean isLeasedBy(String id, String lockId) {
        return isLeased(id) && leases.get(id).getLeasedBy().equals(lockId);
    }

    private static class Item<V> {
        private final V item;

        Item(V item) {
            this.item = item;
        }
    }
}
