/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initiate provider process
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.InitiateTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.command.EntityCommand;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class TransferProcessServiceImpl implements TransferProcessService {
    private final TransferProcessStore transferProcessStore;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ContractNegotiationStore contractNegotiationStore;

    public TransferProcessServiceImpl(TransferProcessStore transferProcessStore,
                                      TransactionContext transactionContext,
                                      CommandHandlerRegistry commandHandlerRegistry,
                                      ContractNegotiationStore contractNegotiationStore, QueryValidator queryValidator) {
        this.transferProcessStore = transferProcessStore;
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.contractNegotiationStore = contractNegotiationStore;
        this.queryValidator = queryValidator;
    }

    @Override
    public @Nullable TransferProcess findById(String transferProcessId) {
        return transactionContext.execute(() -> transferProcessStore.findById(transferProcessId));
    }

    @Override
    public ServiceResult<List<TransferProcess>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(queryTransferProcesses(query))
                );
    }

    @Override
    public @Nullable String getState(String transferProcessId) {
        return transactionContext.execute(() -> {
            var process = transferProcessStore.findById(transferProcessId);
            return Optional.ofNullable(process).map(p -> TransferProcessStates.from(p.getState()).name()).orElse(null);
        });
    }

    @Override
    public @NotNull ServiceResult<Void> complete(String transferProcessId) {
        return execute(new CompleteTransferCommand(transferProcessId));
    }

    @Override
    public @NotNull ServiceResult<Void> terminate(TerminateTransferCommand command) {
        return execute(command);
    }

    @Override
    public @NotNull ServiceResult<Void> suspend(SuspendTransferCommand command) {
        return execute(command);
    }

    @Override
    public @NotNull ServiceResult<Void> resume(ResumeTransferCommand command) {
        return execute(command);
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> initiateTransfer(ParticipantContext participantContext, TransferRequest request) {
        var agreement = contractNegotiationStore.findContractAgreement(request.getContractId());
        if (agreement == null) {
            return ServiceResult.badRequest("Contract agreement with id %s not found".formatted(request.getContractId()));
        }

        return transactionContext.execute(() -> commandHandlerRegistry
                .execute(new InitiateTransferCommand(participantContext, request))
                .flatMap(result -> {
                    if (result.succeeded()) {
                        return ServiceResult.success((TransferProcess) result.getContent());
                    } else {
                        return ServiceResult.from(result);
                    }
                }));
    }

    @Override
    public ServiceResult<Void> notifyPrepared(NotifyPreparedCommand command) {
        return execute(command);
    }

    @Override
    public ServiceResult<Void> notifyStarted(NotifyStartedCommand command) {
        return execute(command);
    }

    private List<TransferProcess> queryTransferProcesses(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = transferProcessStore.findAll(query)) {
                return stream.toList();
            }
        });
    }

    private ServiceResult<Void> execute(EntityCommand command) {
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

}
