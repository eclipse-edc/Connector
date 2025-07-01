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

package org.eclipse.edc.connector.controlplane.defaults.storage.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore extends InMemoryStatefulEntityStore<ContractNegotiation> implements ContractNegotiationStore {

    private final QueryResolver<ContractAgreement> agreementQueryResolver;
    private final ReentrantReadWriteLock lock;

    public InMemoryContractNegotiationStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryContractNegotiationStore(String leaseHolder, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(ContractNegotiation.class, leaseHolder, clock, criterionOperatorRegistry, state -> ContractNegotiationStates.valueOf(state).code());
        agreementQueryResolver = new ReflectionBasedQueryResolver<>(ContractAgreement.class, criterionOperatorRegistry);
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        return super.findAll()
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull)
                .filter(a -> Objects.equals(contractId, a.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public StoreResult<Void> deleteById(String negotiationId) {
        lock.writeLock().lock();
        try {
            var existing = findById(negotiationId);
            if (existing == null) {
                return StoreResult.notFound(format("ContractNegotiation %s not found", negotiationId));
            }
            super.delete(negotiationId);
            return StoreResult.success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return super.findAll(querySpec);
    }

    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        return agreementQueryResolver.query(getAgreements(), querySpec);
    }

    @NotNull
    private Stream<ContractAgreement> getAgreements() {
        return super.findAll()
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull);
    }

}
