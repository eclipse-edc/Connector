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

package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.cosmos.policy.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

/**
 * Implementation of the {@link ContractDefinitionStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) hit the cache, while write operations affect both the cache AND the
 * database.
 */
public class CosmosContractDefinitionStore implements ContractDefinitionStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final LockManager lockManager;
    private final String partitionKey;
    private final QueryResolver<ContractDefinition> queryResolver;
    private AtomicReference<Map<String, ContractDefinition>> objectCache;

    public CosmosContractDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String partitionKey) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        lockManager = new LockManager(new ReentrantReadWriteLock(true));
        queryResolver = new ReflectionBasedQueryResolver<>(ContractDefinition.class);
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return getCache().values();
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return lockManager.readLock(() -> queryResolver.query(getCache().values().stream(), spec));
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        lockManager.writeLock(() -> {
            with(retryPolicy).run(() -> cosmosDbApi.saveItems(definitions.stream().map(this::convertToDocument).collect(Collectors.toList())));
            definitions.forEach(this::storeInCache);
            return null;
        });
    }

    @Override
    public void save(ContractDefinition definition) {
        lockManager.writeLock(() -> {
            with(retryPolicy).run(() -> cosmosDbApi.saveItem(convertToDocument(definition)));
            storeInCache(definition);
            return null;
        });
    }

    @Override
    public void update(ContractDefinition definition) {
        lockManager.writeLock(() -> {
            save(definition); //cosmos db api internally uses "upsert" semantics
            storeInCache(definition);
            return null;
        });
    }

    @Override
    public ContractDefinition deleteById(String id) {
        var deletedItem = cosmosDbApi.deleteItem(id);
        return deletedItem == null ? null : convert(deletedItem);
    }

    @Override
    public void reload() {
        lockManager.readLock(() -> {
            // this reloads ALL items from the database. We might want something more elaborate in the future, especially
            // if large amounts of ContractDefinitions need to be held in memory
            var databaseObjects = with(retryPolicy)
                    .get((CheckedSupplier<List<Object>>) cosmosDbApi::queryAllItems)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toMap(ContractDefinition::getId, cd -> cd));

            if (objectCache == null) {
                objectCache = new AtomicReference<>(new HashMap<>());
            }
            objectCache.set(databaseObjects);
            return null;
        });
    }

    private void storeInCache(ContractDefinition definition) {
        getCache().put(definition.getId(), definition);
    }

    @NotNull
    private ContractDefinitionDocument convertToDocument(ContractDefinition def) {
        return new ContractDefinitionDocument(def, partitionKey);
    }

    private Map<String, ContractDefinition> getCache() {
        if (objectCache == null) {
            objectCache = new AtomicReference<>(new HashMap<>());
            reload();
        }
        return objectCache.get();
    }

    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
