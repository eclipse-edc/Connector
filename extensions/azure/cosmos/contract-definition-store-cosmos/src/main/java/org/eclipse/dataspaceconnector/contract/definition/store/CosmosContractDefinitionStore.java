package org.eclipse.dataspaceconnector.contract.definition.store;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;
import static org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil.propertyComparator;

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
    private AtomicReference<Map<String, ContractDefinition>> objectCache;

    public CosmosContractDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;

        lockManager = new LockManager(new ReentrantReadWriteLock(true));
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return getCache().values();
    }

    /**
     * Note: will return the entire stream when the sortField of the QuerySpec refers to a non-existent property
     */
    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return lockManager.readLock(() -> {
            var stream = getCache().values().stream();

            //filter
            var andPredicate = spec.getFilterExpression().stream().map(this::toPredicate).reduce(x -> true, Predicate::and);
            stream = stream.filter(andPredicate);

            //sort
            var sortField = spec.getSortField();

            if (sortField != null) {
                // if the sortfield doesn't exist on the object -> return empty
                if (ReflectionUtil.getFieldRecursive(ContractDefinition.class, sortField) == null) {
                    return Stream.empty();
                }
                var comparator = propertyComparator(spec.getSortOrder() == SortOrder.ASC, sortField);
                stream = stream.sorted(comparator);
            }

            // limit
            return stream.skip(spec.getOffset()).limit(spec.getLimit());
        });
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
    public void delete(String id) {
        cosmosDbApi.deleteItem(id);
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
        return new ContractDefinitionDocument(def);
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

    private Predicate<ContractDefinition> toPredicate(Criterion criterion) {
        return new ContractDefinitionPredicateConverter().convert(criterion);
    }


    private static class ContractDefinitionPredicateConverter extends BaseCriterionToPredicateConverter<ContractDefinition> {
        @Override
        protected <R> R property(String key, Object object) {
            return ReflectionUtil.getFieldValueSilent(key, object);
        }
    }
}
