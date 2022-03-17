/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service;

import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.contract.negotiation.command.commands.CancelNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class ContractNegotiationServiceImpl implements ContractNegotiationService {

    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager manager;
    private final TransactionContext transactionContext;

    public ContractNegotiationServiceImpl(ContractNegotiationStore store, ConsumerContractNegotiationManager manager, TransactionContext transactionContext) {
        this.store = store;
        this.manager = manager;
        this.transactionContext = transactionContext;
    }

    @Override
    public ContractNegotiation findbyId(String contractNegotiationId) {
        return store.find(contractNegotiationId);
    }

    @Override
    public Collection<ContractNegotiation> query(QuerySpec query) {
        return store.queryNegotiations(query).collect(toList());
    }

    @Override
    public String getState(String negotiationId) {
        return Optional.of(negotiationId)
                .map(this::findbyId)
                .map(ContractNegotiation::getState)
                .map(ContractNegotiationStates::from)
                .map(Enum::name)
                .orElse(null);
    }

    @Override
    public ContractAgreement getForNegotiation(String negotiationId) {
        return store.findContractAgreement(negotiationId);
    }

    @Override
    public ContractNegotiation initiateNegotiation(ContractOfferRequest request) {
        var result = manager.initiate(request);
        return result.getContent();
    }

    @Override
    public ServiceResult<ContractNegotiation> cancel(String negotiationId) {
        var result = new AtomicReference<ServiceResult<ContractNegotiation>>();

        transactionContext.execute(() -> {
            var negotiation = store.find(negotiationId);
            if (negotiation == null) {
                result.set(ServiceResult.notFound(format("ContractNegotiation %s does not exist", negotiationId)));
                return;
            }
            manager.enqueueCommand(new CancelNegotiationCommand(negotiationId));
            result.set(ServiceResult.success(negotiation));
        });

        return result.get();
    }

    @Override
    public ServiceResult<ContractNegotiation> decline(String negotiationId) {
        var result = new AtomicReference<ServiceResult<ContractNegotiation>>();

        transactionContext.execute(() -> {
            try {
                var declineResult = manager.declined(ClaimToken.Builder.newInstance().build(), negotiationId);
                if (declineResult.succeeded()) {
                    result.set(ServiceResult.success(declineResult.getContent()));
                } else {
                    result.set(ServiceResult.conflict(format("Cannot decline ContractNegotiation %s", negotiationId)));
                }
            } catch (Exception e) {
                result.set(ServiceResult.conflict(format("Cannot decline ContractNegotiation %s: %s", negotiationId, e.getLocalizedMessage())));
            }
        });

        return result.get();
    }
}
