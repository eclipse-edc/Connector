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

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {
    private static final int TIMEOUT = 1000;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, ContractNegotiation> processesById = new HashMap<>();
    private final Map<String, ContractNegotiation> processesByCorrelationId = new HashMap<>();
    private final Map<String, ContractNegotiation> contractAgreements = new HashMap<>();
    private final Map<Integer, List<ContractNegotiation>> stateCache = new HashMap<>();

    @Override
    public ContractNegotiation find(String id) {
        return readLock(() -> processesById.get(id));
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
        writeLock(() -> {
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
        writeLock(() -> {
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
        return readLock(() -> {
            var set = stateCache.get(state);
            return set == null ? Collections.emptyList() : set.stream()
                    .sorted(Comparator.comparingLong(ContractNegotiation::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .map(ContractNegotiation::copy)
                    .collect(toList());
        });
    }

    private <T> T readLock(Supplier<T> work) {
        try {
            if (!lock.readLock().tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new EdcException("Timeout acquiring read lock");
            }
            try {
                return work.get();
            } finally {
                lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new EdcException(e);
        }
    }

    private <T> T writeLock(Supplier<T> work) {
        try {
            if (!lock.writeLock().tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new EdcException("Timeout acquiring write lock");
            }
            try {
                return work.get();
            } finally {
                lock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new EdcException(e);
        }
    }

}
