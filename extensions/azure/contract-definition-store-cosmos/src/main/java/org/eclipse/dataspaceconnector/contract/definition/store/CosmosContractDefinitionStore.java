package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosContractDefinitionStore implements ContractDefinitionStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final AtomicReference<Map<String, ContractDefinition>> objectCache;
    private final ReentrantReadWriteLock lock; //used to synchronize write operations to the cache and the DB

    public CosmosContractDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        objectCache = new AtomicReference<>(new ConcurrentHashMap<>());
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return objectCache.get().values();
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        lock.writeLock().lock();
        try {
            cosmosDbApi.createItems(definitions.stream().map(this::convertToDocument).collect(Collectors.toList()));
            definitions.forEach(this::storeInCache);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void save(ContractDefinition definition) {
        lock.writeLock().lock();
        try {
            cosmosDbApi.createItem(convertToDocument(definition));
            storeInCache(definition);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(ContractDefinition definition) {
        lock.writeLock().lock();
        try {
            save(definition); //cosmos db api internally uses "upsert" semantics
            storeInCache(definition);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String id) {
        cosmosDbApi.deleteItem(id);
    }

    @Override
    public void reload() {
        lock.readLock().lock();

        try {
            var databaseObjects = with(retryPolicy).get((CheckedSupplier<List<Object>>) cosmosDbApi::queryAllItems)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toMap(ContractDefinition::getId, cd -> cd));

            objectCache.set(databaseObjects);
        } finally {
            lock.readLock().unlock();
        }

    }

    private void storeInCache(ContractDefinition definition) {
        objectCache.get().put(definition.getId(), definition);
    }

    @NotNull
    private ContractDefinitionDocument convertToDocument(ContractDefinition def) {
        return new ContractDefinitionDocument(def);
    }


    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
