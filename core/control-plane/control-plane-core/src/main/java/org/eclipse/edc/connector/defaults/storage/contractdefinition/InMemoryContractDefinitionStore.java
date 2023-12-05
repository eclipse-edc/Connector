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
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.connector.defaults.storage.contractdefinition;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * The default store implementation used when no extension is configured in a runtime. {@link ContractDefinition}s are
 * stored ephemerally in memory.
 */
public class InMemoryContractDefinitionStore implements ContractDefinitionStore {
    private final Map<String, ContractDefinition> cache = new ConcurrentHashMap<>();
    private final CriterionToContractDefinitionPredicateConverterImpl predicateConverter = new CriterionToContractDefinitionPredicateConverterImpl();

    private final ReentrantReadWriteLock lock;

    public InMemoryContractDefinitionStore() {
        // fair locks guarantee strong consistency since all waiting threads are processed in order of waiting time
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {

        lock.readLock().lock();
        try {
            var comparator = spec.getSortField() == null
                    ? (Comparator<ContractDefinition>) (o1, o2) -> 0
                    : new ContractDefinitionComparator(spec.getSortField(), spec.getSortOrder());

            return filterBy(spec.getFilterExpression())
                    .sorted(comparator)
                    .skip(spec.getOffset()).limit(spec.getLimit());

        } finally {
            lock.readLock().unlock();
        }
    }

    private Stream<ContractDefinition> filterBy(List<Criterion> criteria) {
        var predicate = criteria.stream()
                .map(predicateConverter::convert)
                .reduce(x -> true, Predicate::and);

        return cache.values().stream()
                .filter(predicate);
    }

    @Override
    public ContractDefinition findById(String definitionId) {
        return cache.get(definitionId);
    }


    @Override
    public StoreResult<Void> save(ContractDefinition definition) {
        var prev = cache.putIfAbsent(definition.getId(), definition);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>alreadyExists(format(CONTRACT_DEFINITION_EXISTS, definition.getId())))
                .orElse(StoreResult.success());
    }

    @Override
    public StoreResult<Void> update(ContractDefinition definition) {
        var prev = cache.replace(definition.getId(), definition);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, definition.getId())));
    }

    @Override
    public StoreResult<ContractDefinition> deleteById(String id) {
        var prev = cache.remove(id);
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, id)));
    }

    private record ContractDefinitionComparator(String sortField,
                                                SortOrder sortOrder) implements Comparator<ContractDefinition> {

        @Override
        public int compare(ContractDefinition contractDefinition1, ContractDefinition contractDefinition2) {
            Comparable<Object> f1 = null;
            Comparable<Object> f2 = null;
            if (sortField.equals("id")) {
                f1 = asComparable(contractDefinition1.getId());
                f2 = asComparable(contractDefinition2.getId());
            } else if (sortField.equals("accessPolicyId")) {
                f1 = asComparable(contractDefinition1.getAccessPolicyId());
                f2 = asComparable(contractDefinition2.getAccessPolicyId());
            } else if (sortField.equals("contractPolicyId")) {
                f1 = asComparable(contractDefinition1.getContractPolicyId());
                f2 = asComparable(contractDefinition2.getContractPolicyId());
            }

            if (f1 == null || f2 == null) {
                throw new IllegalArgumentException(format("Cannot sort by field %s, it does not exist on one or more Contract definitions", sortField));
            }
            return sortOrder == SortOrder.ASC ? f1.compareTo(f2) : f2.compareTo(f1);
        }

        private @Nullable Comparable<Object> asComparable(Object property) {
            return property instanceof Comparable ? (Comparable<Object>) property : null;
        }
    }

}
