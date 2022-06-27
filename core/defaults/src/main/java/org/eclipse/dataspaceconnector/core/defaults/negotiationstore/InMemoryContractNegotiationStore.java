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

package org.eclipse.dataspaceconnector.core.defaults.negotiationstore;

import org.eclipse.dataspaceconnector.core.defaults.InMemoryStatefulEntityStore;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore implements ContractNegotiationStore {

    private final InMemoryStatefulEntityStore<ContractNegotiation> store = new InMemoryStatefulEntityStore<>(ContractNegotiation.class);
    private final QueryResolver<ContractNegotiation> negotiationQueryResolver = new ReflectionBasedQueryResolver<>(ContractNegotiation.class);
    private final QueryResolver<ContractAgreement> agreementQueryResolver = new ReflectionBasedQueryResolver<>(ContractAgreement.class);

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
        return store.findAll().filter(p -> contractId.equals(p.getContractAgreement().getId())).findFirst().map(ContractNegotiation::getContractAgreement).orElse(null);
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        store.upsert(negotiation);
    }

    @Override
    public void delete(String negotiationId) {
        store.delete(negotiationId);
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return negotiationQueryResolver.query(store.findAll(), querySpec);
    }

    @Override
    public @NotNull Stream<ContractAgreement> getAgreementsForDefinitionId(String definitionId) {
        return getAgreements().filter(it -> it.getId().startsWith(definitionId + ":"));
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
