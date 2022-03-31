/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.cosmos.policy.store;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

/**
 * Implementation of the {@link PolicyStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) always hit the cache, while write operations affect both the cache AND the
 * database.
 */
public class CosmosPolicyStore implements PolicyStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final LockManager lockManager;
    private final String partitionKey;
    private final QueryResolver<Policy> queryResolver;
    private final AtomicReference<Map<String, Policy>> objectCache;

    public CosmosPolicyStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String partitionKey) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        lockManager = new LockManager(new ReentrantReadWriteLock(true));
        queryResolver = new ReflectionBasedQueryResolver<>(Policy.class);
        objectCache = new AtomicReference<>(new HashMap<>());
    }

    @Override
    public @Nullable Policy findById(String policyId) {
        return lockManager.readLock(() -> getCache().get(policyId));
    }

    @Override
    public Stream<Policy> findAll(QuerySpec spec) {
        return lockManager.readLock(() -> queryResolver.query(getCache().values().stream(), spec));
    }

    @Override
    public void save(Policy policy) {
        lockManager.writeLock(() -> {
            with(retryPolicy).run(() -> cosmosDbApi.saveItem(convertToDocument(policy)));
            storeInCache(policy);
            return null;
        });
    }

    @Override
    public @Nullable Policy deleteById(String policyId) {
        return lockManager.writeLock(() -> {
            var deletedItem = cosmosDbApi.deleteItem(policyId);
            if (deletedItem == null) {
                return null;
            }
            removeFromCache(policyId);
            return convert(deletedItem);
        });
    }

    @Override
    public void reload() {
        lockManager.readLock(() -> {
            // this reloads ALL items from the database. We might want something more elaborate in the future, especially
            // if large amounts of ContractDefinitions need to be held in memory
            var databaseObjects = with(retryPolicy)
                    .get(() -> cosmosDbApi.queryAllItems())
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toMap(Policy::getUid, cd -> cd));

            objectCache.set(databaseObjects);
            return null;
        });
    }

    private Policy removeFromCache(String policyId) {
        return lockManager.readLock(() -> {
            var map = getCache();
            return map.remove(policyId);
        });

    }

    private void storeInCache(Policy definition) {
        getCache().put(definition.getUid(), definition);
    }

    @NotNull
    private PolicyDocument convertToDocument(Policy policy) {
        return new PolicyDocument(policy, partitionKey);
    }

    private Map<String, Policy> getCache() {
        if (objectCache.get().isEmpty()) {
            reload();
        }
        return objectCache.get();
    }

    private Policy convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, PolicyDocument.class).getWrappedInstance();
    }
}
