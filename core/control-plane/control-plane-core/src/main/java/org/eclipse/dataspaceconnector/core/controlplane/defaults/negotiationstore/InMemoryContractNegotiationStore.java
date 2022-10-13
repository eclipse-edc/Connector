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

package org.eclipse.dataspaceconnector.core.controlplane.defaults.negotiationstore;

import org.eclipse.dataspaceconnector.core.controlplane.defaults.InMemoryStatefulEntityStore;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.persistence.Lease;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {

    private final QueryResolver<ContractNegotiation> negotiationQueryResolver = new ReflectionBasedQueryResolver<>(ContractNegotiation.class);
    private final QueryResolver<ContractAgreement> agreementQueryResolver = new ReflectionBasedQueryResolver<>(ContractAgreement.class);
    private final InMemoryStatefulEntityStore<ContractNegotiation> store;

    public InMemoryContractNegotiationStore() {
        this(UUID.randomUUID().toString(), Clock.systemUTC(), new HashMap<>());
    }

    public InMemoryContractNegotiationStore(String leaseHolder, Clock clock, Map<String, Lease> leases) {
        store = new InMemoryStatefulEntityStore<>(ContractNegotiation.class, leaseHolder, clock, leases);
    }

    @Override
    public @Nullable ContractNegotiation find(String negotiationId) {
        return store.find(negotiationId);
    }

    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        return store.findAll().filter(p -> correlationId.equals(p.getCorrelationId())).findFirst().orElse(null);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        return store.findAll()
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull)
                .filter(a -> Objects.equals(contractId, a.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        store.upsert(negotiation);
    }

    @Override
    public void delete(String negotiationId) {
        var negotiation = store.find(negotiationId);
        if (negotiation != null && negotiation.getContractAgreement() != null) {
            throw new IllegalStateException(format("Cannot delete ContractNegotiation [%s]: ContractAgreement already created.", negotiationId));
        }
        store.delete(negotiationId);
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return negotiationQueryResolver.query(store.findAll(), querySpec);
    }


    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        return agreementQueryResolver.query(getAgreements(), querySpec);
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return store.nextForState(state, max);
    }

    @NotNull
    private Stream<ContractAgreement> getAgreements() {
        return store.findAll()
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull);
    }
}
