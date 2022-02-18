/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.negotiation.store.memory.ContractNegotiationFunctions.property;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final Map<String, ContractNegotiation> processesById = new HashMap<>();
    private final Map<String, ContractNegotiation> processesByCorrelationId = new HashMap<>();
    private final Map<String, ContractNegotiation> contractAgreements = new HashMap<>();
    private final Map<Integer, List<ContractNegotiation>> stateCache = new HashMap<>();

    @Override
    public ContractNegotiation find(String id) {
        return lockManager.readLock(() -> processesById.get(id));
    }

    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        var process = processesByCorrelationId.get(correlationId);
        var processId = process != null ? process.getId() : null;
        return find(processId);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        var negotiation = contractAgreements.get(contractId);
        return negotiation != null ? negotiation.getContractAgreement() : null;
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        lockManager.writeLock(() -> {
            negotiation.updateStateTimestamp();
            delete(negotiation.getId());
            ContractNegotiation internalCopy = negotiation.copy();
            processesById.put(negotiation.getId(), internalCopy);
            processesByCorrelationId.put(negotiation.getCorrelationId(), internalCopy);
            var agreement = internalCopy.getContractAgreement();
            if (agreement != null) {
                contractAgreements.put(agreement.getId(), internalCopy);
            }
            stateCache.computeIfAbsent(negotiation.getState(), k -> new ArrayList<>()).add(internalCopy);
            return null;
        });
    }

    @Override
    public void delete(String processId) {
        lockManager.writeLock(() -> {
            ContractNegotiation process = processesById.remove(processId);
            if (process != null) {
                var tempCache = new HashMap<Integer, List<ContractNegotiation>>();
                stateCache.forEach((key, value) -> {
                    var list = value.stream().filter(p -> !p.getId().equals(processId)).collect(Collectors.toCollection(ArrayList::new));
                    tempCache.put(key, list);
                });
                stateCache.clear();
                stateCache.putAll(tempCache);
                processesByCorrelationId.remove(process.getCorrelationId());

                if (process.getContractAgreement() != null) {
                    contractAgreements.remove(process.getContractAgreement().getId());
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return lockManager.readLock(() -> {
            var set = stateCache.get(state);
            return set == null ? Collections.emptyList() : set.stream()
                    .sorted(Comparator.comparingLong(ContractNegotiation::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .map(ContractNegotiation::copy)
                    .collect(toList());
        });
    }

    @Override
    public Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return lockManager.readLock(() -> {
            Stream<ContractNegotiation> negotiationStream = processesById.values().stream();
            // filter
            var andPredicate = querySpec.getFilterExpression().stream().map(this::toPredicate).reduce(x -> true, Predicate::and);
            negotiationStream = negotiationStream.filter(andPredicate);

            // sort
            var sortField = querySpec.getSortField();

            if (sortField != null) {
                var comparator = propertyComparator(querySpec, sortField);
                negotiationStream = negotiationStream.sorted(comparator);
            }

            //limit
            negotiationStream = negotiationStream.skip(querySpec.getOffset()).limit(querySpec.getLimit());

            return negotiationStream;
        });
    }

    @NotNull
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparator<ContractNegotiation> propertyComparator(QuerySpec querySpec, String property) {
        return (negotiation1, negotiation2) -> {
            var o1 = property(negotiation1, property);
            var o2 = property(negotiation2, property);

            if (o1 == null || o2 == null) {
                return 0;
            }

            if (!(o1 instanceof Comparable)) {
                throw new IllegalArgumentException("A property '" + property + "' is not comparable!");
            }
            var comp1 = (Comparable) o1;
            var comp2 = (Comparable) o2;
            return querySpec.getSortOrder() == SortOrder.ASC ? comp1.compareTo(comp2) : comp2.compareTo(comp1);
        };
    }

    private Predicate<ContractNegotiation> toPredicate(Criterion criterion) {
        return new ContractNegotiationPredicateConverter().convert(criterion);
    }


}
