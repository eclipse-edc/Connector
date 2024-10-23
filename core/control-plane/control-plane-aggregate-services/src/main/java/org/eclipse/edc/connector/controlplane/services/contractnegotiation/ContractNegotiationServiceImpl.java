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

package org.eclipse.edc.connector.controlplane.services.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class ContractNegotiationServiceImpl implements ContractNegotiationService {

    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager consumerManager;
    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final QueryValidator queryValidator;

    public ContractNegotiationServiceImpl(ContractNegotiationStore store, ConsumerContractNegotiationManager consumerManager,
                                          TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry,
                                          QueryValidator queryValidator) {
        this.store = store;
        this.consumerManager = consumerManager;
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.queryValidator = queryValidator;
    }

    @Override
    public ContractNegotiation findbyId(String contractNegotiationId) {
        return transactionContext.execute(() -> store.findById(contractNegotiationId));
    }

    @Override
    public ServiceResult<List<ContractNegotiation>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(queryNegotiations(query))
                );
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
        return transactionContext.execute(() -> ofNullable(store.findById(negotiationId))
                .map(ContractNegotiation::getContractAgreement).orElse(null));
    }

    @Override
    public ContractNegotiation initiateNegotiation(ContractRequest request) {
        return transactionContext.execute(() -> consumerManager.initiate(request).getContent());
    }

    @Override
    public ServiceResult<Void> terminate(TerminateNegotiationCommand command) {
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    private List<ContractNegotiation> queryNegotiations(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.queryNegotiations(query)) {
                return stream.toList();
            }
        });
    }

}
