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

package org.eclipse.edc.connector.service.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.command.CancelNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class ContractNegotiationServiceImpl implements ContractNegotiationService {

    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager manager;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;

    public ContractNegotiationServiceImpl(ContractNegotiationStore store, ConsumerContractNegotiationManager manager, TransactionContext transactionContext) {
        this.store = store;
        this.manager = manager;
        this.transactionContext = transactionContext;
        queryValidator = new QueryValidator(ContractNegotiation.class);
    }

    @Override
    public ContractNegotiation findbyId(String contractNegotiationId) {
        return transactionContext.execute(() -> store.find(contractNegotiationId));
    }

    @Override
    public ServiceResult<Stream<ContractNegotiation>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(format("Error validating schema: %s", result.getFailureDetail()));
        }
        return ServiceResult.success(transactionContext.execute(() -> store.queryNegotiations(query)));
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
        return transactionContext.execute(() -> ofNullable(store.find(negotiationId))
                .map(ContractNegotiation::getContractAgreement).orElse(null));
    }

    @Override
    public ContractNegotiation initiateNegotiation(ContractOfferRequest request) {
        return transactionContext.execute(() -> manager.initiate(request).getContent());
    }

    @Override
    public ServiceResult<ContractNegotiation> cancel(String negotiationId) {
        return transactionContext.execute(() -> {
            var negotiation = store.find(negotiationId);
            if (negotiation == null) {
                return ServiceResult.notFound(format("ContractNegotiation %s does not exist", negotiationId));
            } else {
                manager.enqueueCommand(new CancelNegotiationCommand(negotiationId));
                return ServiceResult.success(negotiation);
            }
        });
    }

    @Override
    public ServiceResult<ContractNegotiation> decline(String negotiationId) {
        return transactionContext.execute(() -> {
            try {
                var declineResult = manager.declined(ClaimToken.Builder.newInstance().build(), negotiationId);
                if (declineResult.succeeded()) {
                    return ServiceResult.success(declineResult.getContent());
                } else {
                    return ServiceResult.conflict(format("Cannot decline ContractNegotiation %s", negotiationId));
                }
            } catch (Exception e) {
                return ServiceResult.conflict(format("Cannot decline ContractNegotiation %s: %s", negotiationId, e.getLocalizedMessage()));
            }
        });
    }
}
