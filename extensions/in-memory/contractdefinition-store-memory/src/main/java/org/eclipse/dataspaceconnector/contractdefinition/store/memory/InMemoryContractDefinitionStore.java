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
 *
 */

package org.eclipse.dataspaceconnector.contractdefinition.store.memory;

import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil.propertyComparator;

/**
 * The default store implementation used when no extension is configured in a runtime. {@link ContractDefinition}s are stored ephemerally in memory.
 */
public class InMemoryContractDefinitionStore implements ContractDefinitionStore {
    private final Map<String, ContractDefinition> cache = new ConcurrentHashMap<>();

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        var stream = cache.values().stream();

        //filter
        var andPredicate = spec.getFilterExpression().stream().map(this::toPredicate).reduce(x -> true, Predicate::and);
        stream = stream.filter(andPredicate);

        //sort
        var sortField = spec.getSortField();

        if (sortField != null) {
            if (ReflectionUtil.getFieldRecursive(ContractDefinition.class, sortField) == null) {
                return Stream.empty();
            }
            var comparator = propertyComparator(spec.getSortOrder() == SortOrder.ASC, sortField);
            stream = stream.sorted(comparator);
        }

        // limit
        return stream.skip(spec.getOffset()).limit(spec.getLimit());
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        definitions.forEach(d -> cache.put(d.getId(), d));
    }

    @Override
    public void save(ContractDefinition definition) {
        cache.put(definition.getId(), definition);
    }

    @Override
    public void update(ContractDefinition definition) {
        save(definition);
    }

    @Override
    public ContractDefinition deleteById(String id) {
        return cache.remove(id);
    }

    @Override
    public void reload() {
        // no-op
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
