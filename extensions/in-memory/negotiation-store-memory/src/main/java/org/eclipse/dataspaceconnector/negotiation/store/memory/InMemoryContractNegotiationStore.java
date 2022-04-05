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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
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
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final Map<String, ContractNegotiation> negotiationById = new HashMap<>();
    private final Map<String, ContractNegotiation> negotiationByCorrelationId = new HashMap<>();
    private final Map<String, ContractNegotiation> contractAgreements = new HashMap<>();
    private final Map<Integer, List<ContractNegotiation>> stateCache = new HashMap<>();
    private final QueryResolver<ContractNegotiation> negotiationQueryResolver = new ReflectionBasedQueryResolver<>(ContractNegotiation.class);
    private final QueryResolver<ContractAgreement> agreementQueryResolver = new ReflectionBasedQueryResolver<>(ContractAgreement.class);

    @Override
    public ContractNegotiation find(String id) {
        return lockManager.readLock(() -> negotiationById.get(id));
    }

    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        var process = negotiationByCorrelationId.get(correlationId);
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
            negotiationById.put(negotiation.getId(), internalCopy);
            negotiationByCorrelationId.put(negotiation.getCorrelationId(), internalCopy);
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
            ContractNegotiation process = negotiationById.remove(processId);
            if (process != null) {
                var tempCache = new HashMap<Integer, List<ContractNegotiation>>();
                stateCache.forEach((key, value) -> {
                    var list = value.stream().filter(p -> !p.getId().equals(processId)).collect(Collectors.toCollection(ArrayList::new));
                    tempCache.put(key, list);
                });
                stateCache.clear();
                stateCache.putAll(tempCache);
                negotiationByCorrelationId.remove(process.getCorrelationId());

                if (process.getContractAgreement() != null) {
                    contractAgreements.remove(process.getContractAgreement().getId());
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return lockManager.readLock(() -> negotiationQueryResolver.query(negotiationById.values().stream(), querySpec));
    }

    @Override
    public @NotNull Stream<ContractAgreement> getAgreementsForDefinitionId(String definitionId) {
        return lockManager.readLock(() -> getAgreements().filter(it -> it.getId().startsWith(definitionId + ":")));
    }

    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        return lockManager.readLock(() -> agreementQueryResolver.query(getAgreements(), querySpec));
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return lockManager.readLock(() -> {
            var set = stateCache.get(state);

            List<ContractNegotiation> toBeLeased = set == null ? Collections.emptyList() : set.stream()
                    .sorted(Comparator.comparingLong(ContractNegotiation::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .collect(toList());

            stateCache.compute(state, (key, value) -> {
                if (value != null) {
                    value.removeAll(toBeLeased);
                }
                return value;
            });

            return toBeLeased.stream()
                    .map(ContractNegotiation::copy)
                    .collect(toList());
        });
    }

    @Override
    public Policy findPolicyForContract(String contractId) {
        return ofNullable(findContractAgreement(contractId)).map(ContractAgreement::getPolicy).orElse(null);
    }

    @NotNull
    private Stream<ContractAgreement> getAgreements() {
        return negotiationById.values().stream()
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull);
    }

}
