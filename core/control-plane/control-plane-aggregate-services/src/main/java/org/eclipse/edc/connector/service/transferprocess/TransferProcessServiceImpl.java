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
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.SingleTransferProcessCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.command.CompleteTransferCommand;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransferProcessServiceImpl implements TransferProcessService {
    private final TransferProcessStore transferProcessStore;
    private final TransferProcessManager manager;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;
    private final DataAddressValidator dataAddressValidator;

    public TransferProcessServiceImpl(TransferProcessStore transferProcessStore, TransferProcessManager manager,
                                      TransactionContext transactionContext,
                                      DataAddressValidator dataAddressValidator) {
        this.transferProcessStore = transferProcessStore;
        this.manager = manager;
        this.transactionContext = transactionContext;
        this.dataAddressValidator = dataAddressValidator;
        queryValidator = new QueryValidator(TransferProcess.class, getSubtypes());
    }

    @Override
    public @Nullable TransferProcess findById(String transferProcessId) {
        return transactionContext.execute(() -> transferProcessStore.findById(transferProcessId));
    }

    @Override
    public ServiceResult<Stream<TransferProcess>> query(QuerySpec query) {
        var result = queryValidator.validate(query);

        if (result.failed()) {
            return ServiceResult.badRequest(format("Error validating schema: %s", result.getFailureDetail()));
        }
        return ServiceResult.success(transactionContext.execute(() -> transferProcessStore.findAll(query)));
    }

    @Override
    public @Nullable String getState(String transferProcessId) {
        return transactionContext.execute(() -> {
            var process = transferProcessStore.findById(transferProcessId);
            return Optional.ofNullable(process).map(p -> TransferProcessStates.from(p.getState()).name()).orElse(null);
        });
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> terminate(String transferProcessId, String reason) {
        return transactionContext.execute(() -> runAsync(new TerminateTransferCommand(transferProcessId, reason)));
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> complete(String transferProcessId) {
        return transactionContext.execute(() -> runAsync(new CompleteTransferCommand(transferProcessId)));
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> deprovision(String transferProcessId) {
        return transactionContext.execute(() -> runAsync(new DeprovisionRequest(transferProcessId)));
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> initiateTransfer(TransferRequest request) {
        var validDestination = dataAddressValidator.validate(request.getDataRequest().getDataDestination());
        if (validDestination.failed()) {
            return ServiceResult.badRequest(validDestination.getFailureMessages().toArray(new String[]{}));
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
    public ServiceResult<TransferProcess> completeDeprovision(String transferProcessId, DeprovisionedResource resource) {
        return transactionContext.execute(() -> runAsync(new DeprovisionCompleteCommand(transferProcessId, resource)));
    }

    @Override
    public ServiceResult<TransferProcess> addProvisionedResource(String transferProcessId, ProvisionResponse response) {
        return transactionContext.execute(() -> runAsync(new AddProvisionedResourceCommand(transferProcessId, response)));
    }

    private ServiceResult<TransferProcess> runAsync(SingleTransferProcessCommand command) {
        return Optional.of(command.getTransferProcessId())
                .map(transferProcessStore::findById)
                .map(transferProcess -> {
                    var validator = asyncCommandValidators.get(command.getClass());
                    var validationResult = validator.apply(command, transferProcess);
                    if (validationResult.failed()) {
                        return ServiceResult.<TransferProcess>conflict(format("Cannot %s because %s", command.getClass().getSimpleName(), validationResult.getFailureDetail()));
                    }

                    manager.enqueueCommand(command);
                    return ServiceResult.success(transferProcess);
                })
                .orElse(ServiceResult.notFound(format("TransferProcess with id %s not found", command.getTransferProcessId())));
    }

    private Map<Class<?>, List<Class<?>>> getSubtypes() {
        return Map.of(
                ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                ProvisionedDataAddressResource.class, List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)
        );
    }

    private final Map<Class<? extends SingleTransferProcessCommand>, CommandValidator> asyncCommandValidators = Map.of(
            TerminateTransferCommand.class,
            (command, transferProcess) -> transferProcess.canBeTerminated()
                    ? Result.success()
                    : Result.failure(format("TransferProcess %s is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState()))),

            org.eclipse.edc.spi.types.domain.transfer.command.CompleteTransferCommand.class,
            (command, transferProcess) -> transferProcess.canBeCompleted()
                    ? Result.success()
                    : Result.failure(format("TransferProcess %s is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState()))),

            DeprovisionRequest.class,
            (command, transferProcess) -> transferProcess.canBeDeprovisioned()
                    ? Result.success()
                    : Result.failure(format("TransferProcess %s is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState()))),

            DeprovisionCompleteCommand.class, (command, transferProcess) -> Result.success(),

            AddProvisionedResourceCommand.class, (command, transferProcess) -> Result.success()
    );

    private interface CommandValidator extends BiFunction<SingleTransferProcessCommand, TransferProcess, Result<Void>> {}
}
