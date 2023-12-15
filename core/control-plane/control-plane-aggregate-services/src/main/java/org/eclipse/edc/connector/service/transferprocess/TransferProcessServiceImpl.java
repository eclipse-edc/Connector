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
 *       Fraunhofer Institute for Software and Systems Engineering - initiate provider process
 *
 */

package org.eclipse.edc.connector.service.transferprocess;

import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.AddProvisionedResourceCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.CompleteTransferCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class TransferProcessServiceImpl implements TransferProcessService {
    private final TransferProcessStore transferProcessStore;
    private final TransferProcessManager manager;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;
    private final DataAddressValidatorRegistry dataAddressValidator;
    private final CommandHandlerRegistry commandHandlerRegistry;

    public TransferProcessServiceImpl(TransferProcessStore transferProcessStore, TransferProcessManager manager,
                                      TransactionContext transactionContext, DataAddressValidatorRegistry dataAddressValidator,
                                      CommandHandlerRegistry commandHandlerRegistry) {
        this.transferProcessStore = transferProcessStore;
        this.manager = manager;
        this.transactionContext = transactionContext;
        this.dataAddressValidator = dataAddressValidator;
        this.commandHandlerRegistry = commandHandlerRegistry;
        queryValidator = new QueryValidator(TransferProcess.class, getSubtypes());
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
        var command = new CompleteTransferCommand(transferProcessId);
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    @Override
    public @NotNull ServiceResult<Void> terminate(TerminateTransferCommand command) {
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    @Override
    public @NotNull ServiceResult<Void> deprovision(String transferProcessId) {
        var command = new DeprovisionRequest(transferProcessId);
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> initiateTransfer(TransferRequest request) {
        var validDestination = dataAddressValidator.validateDestination(request.getDataDestination());
        if (validDestination.failed()) {
            return ServiceResult.badRequest(validDestination.getFailureMessages());
        }

        return transactionContext.execute(() -> {
            var transferInitiateResult = manager.initiateConsumerRequest(request);
            return Optional.ofNullable(transferInitiateResult)
                    .filter(AbstractResult::succeeded)
                    .map(AbstractResult::getContent)
                    .map(ServiceResult::success)
                    .orElse(ServiceResult.conflict("Request couldn't be initialised."));
        });
    }

    @Override
    public ServiceResult<Void> completeDeprovision(String transferProcessId, DeprovisionedResource resource) {
        var command = new DeprovisionCompleteCommand(transferProcessId, resource);
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<Void> addProvisionedResource(String transferProcessId, ProvisionResponse response) {
        var command = new AddProvisionedResourceCommand(transferProcessId, response);
        return transactionContext.execute(() -> commandHandlerRegistry.execute(command).flatMap(ServiceResult::from));
    }

    private List<TransferProcess> queryTransferProcesses(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = transferProcessStore.findAll(query)) {
                return stream.toList();
            }
        });
    }

    private Map<Class<?>, List<Class<?>>> getSubtypes() {
        return Map.of(
                ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                ProvisionedDataAddressResource.class, List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)
        );
    }

}
