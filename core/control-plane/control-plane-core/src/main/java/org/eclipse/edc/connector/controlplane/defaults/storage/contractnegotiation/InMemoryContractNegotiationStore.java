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
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.entity.StateResolver;
import org.eclipse.edc.spi.query.CriteriaToPredicate;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.AndOperatorCriteriaToPredicate;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.store.StatefulEntityCriteriaToPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.partitioningBy;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.LATEST_CONTRACT_OFFER_PROPERTY;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryContractNegotiationStore extends InMemoryStatefulEntityStore<ContractNegotiation> implements ContractNegotiationStore {

    private static final String LATEST_CONTRACT_OFFER_PREFIX = LATEST_CONTRACT_OFFER_PROPERTY + ".";

    private final QueryResolver<ContractAgreement> agreementQueryResolver;
    private final QueryResolver<ContractNegotiation> negotiationQueryResolver;
    private final CriteriaToPredicate<ContractOffer> contractOfferCriteriaToPredicate;
    private final ReentrantReadWriteLock lock;

    public InMemoryContractNegotiationStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryContractNegotiationStore(String leaseHolder, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(leaseHolder, clock, criterionOperatorRegistry, state -> ContractNegotiationStates.valueOf(state).code());
    }

    private InMemoryContractNegotiationStore(String leaseHolder, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry, StateResolver stateResolver) {
        super(ContractNegotiation.class, leaseHolder, clock, criterionOperatorRegistry, stateResolver);
        agreementQueryResolver = new ReflectionBasedQueryResolver<>(ContractAgreement.class, criterionOperatorRegistry);
        negotiationQueryResolver = new ReflectionBasedQueryResolver<>(ContractNegotiation.class, new StatefulEntityCriteriaToPredicate<>(criterionOperatorRegistry, stateResolver));
        contractOfferCriteriaToPredicate = new AndOperatorCriteriaToPredicate<>(criterionOperatorRegistry);
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
            return super.delete(negotiationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        var criteria = querySpec.getFilterExpression().stream()
                .collect(partitioningBy(c -> c.getOperandLeft().toString().startsWith(LATEST_CONTRACT_OFFER_PREFIX)));
        var latestContractOfferCriteria = criteria.get(true);
        if (latestContractOfferCriteria.isEmpty()) {
            return super.findAll(querySpec);
        }

        // latestContractOffer is a virtual property, it cannot be resolved through reflection on the negotiation
        var latestContractOfferPredicate = contractOfferCriteriaToPredicate.convert(latestContractOfferCriteria.stream()
                .map(c -> c.withLeftOperand(c.getOperandLeft().toString().substring(LATEST_CONTRACT_OFFER_PREFIX.length())))
                .toList());
        var candidates = super.findAll().filter(negotiation -> {
            var latestContractOffer = negotiation.getLastContractOffer();
            return latestContractOffer != null && latestContractOfferPredicate.test(latestContractOffer);
        });
        var remainingQuery = QuerySpec.Builder.newInstance()
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .sortField(querySpec.getSortField())
                .sortOrder(querySpec.getSortOrder())
                .filter(criteria.get(false))
                .build();
        return negotiationQueryResolver.query(candidates, remainingQuery);
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
