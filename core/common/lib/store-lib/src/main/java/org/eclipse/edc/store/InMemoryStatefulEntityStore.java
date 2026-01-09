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

package org.eclipse.edc.store;

import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.util.concurrency.LockManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe entity store for a {@link StatefulEntity}. This implementation is intended for testing
 * purposes only.
 */
public class InMemoryStatefulEntityStore<T extends StatefulEntity<T>> implements StateEntityStore<T> {
    private static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(60);
    protected final CriterionOperatorRegistry criterionOperatorRegistry;
    private final Map<String, T> entitiesById = new ConcurrentHashMap<>();
    private final QueryResolver<T> queryResolver;
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final String lockId;
    private final Clock clock;
    private final Map<String, Lease> leases = new HashMap<>();

    public InMemoryStatefulEntityStore(Class<T> clazz, String lockId, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry, StateResolver stateResolver) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(clazz, new StatefulEntityCriteriaToPredicate<>(criterionOperatorRegistry, stateResolver));
        this.lockId = lockId;
        this.clock = clock;
        this.criterionOperatorRegistry = criterionOperatorRegistry;
    }

    @Override
    public @Nullable T findById(String id) {
        var t = entitiesById.get(id);
        if (t == null) {
            return null;
        }
        return t.copy();
    }

    @Override
    public @NotNull List<T> nextNotLeased(int max, Criterion... criteria) {
        return lockManager.writeLock(() -> {
            var filterPredicate = Arrays.stream(criteria).map(criterionOperatorRegistry::toPredicate).reduce(x -> true, Predicate::and);
            var entities = entitiesById.values().stream()
                    .filter(filterPredicate)
                    .filter(e -> !isLeased(e.getId()))
                    .sorted(comparingLong(StatefulEntity::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .toList();
            entities.forEach(i -> acquireLease(i.getId()));
            return entities.stream().map(StatefulEntity::copy).collect(toList());
        });
    }

    @Override
    public StoreResult<T> findByIdAndLease(String id) {
        return lockManager.writeLock(() -> {
            var entity = entitiesById.get(id);
            if (entity == null) {
                return StoreResult.notFound(format("Entity %s not found", id));
            }
            if (isLeased(id)) {
                return StoreResult.alreadyLeased("Entity %s already leased".formatted(id));
            }
            return acquireLease(id).map(it -> entity.copy());
        });
    }

    @Override
    public StoreResult<Void> save(T entity) {
        return acquireLease(entity.getId()).compose(it -> {
            entitiesById.put(entity.getId(), entity.copy());
            freeLease(entity.getId());
            return StoreResult.success();
        });

    }

    public StoreResult<Void> delete(String id) {
        if (isLeased(id)) {
            return StoreResult.alreadyLeased("Entity is leased and cannot be deleted!");
        }
        entitiesById.remove(id);
        return StoreResult.success();
    }

    public Stream<T> findAll(QuerySpec querySpec) {
        return queryResolver.query(findAll(), querySpec);
    }

    public Stream<T> findAll() {
        return entitiesById.values().stream();
    }

    public StoreResult<Void> acquireLease(String id, String lockId, Duration leaseTime) {
        if (!isLeased(id) || isLeasedBy(id, lockId)) {
            leases.put(id, new Lease(lockId, clock.millis(), leaseTime.toMillis()));
            return StoreResult.success();
        } else {
            return StoreResult.alreadyLeased("Cannot acquire lease, is already leased by someone else!");
        }
    }

    public boolean isLeasedBy(String id, String lockId) {
        return isLeased(id) && leases.get(id).getLeasedBy().equals(lockId);
    }

    private void freeLease(String id) {
        leases.remove(id);
    }

    private StoreResult<Void> acquireLease(String id) {
        return acquireLease(id, lockId, DEFAULT_LEASE_TIME);
    }

    private boolean isLeased(String id) {
        return leases.containsKey(id) && !leases.get(id).isExpired(clock.millis());
    }

}
