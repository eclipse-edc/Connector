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

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.service.query.QueryValidator;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.AddProvisionedResourceCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.CancelTransferCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.FailTransferCommand;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.types.domain.transfer.command.CompleteTransferCommand;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransferProcessServiceImpl implements TransferProcessService {
    private final TransferProcessStore transferProcessStore;
    private final TransferProcessManager manager;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;
    private final ContractNegotiationStore negotiationStore;
    private final ContractValidationService contractValidationService;
    private final DataAddressValidator dataAddressValidator;

    public TransferProcessServiceImpl(TransferProcessStore transferProcessStore, TransferProcessManager manager,
                                      TransactionContext transactionContext, ContractNegotiationStore negotiationStore,
                                      ContractValidationService contractValidationService,
                                      DataAddressValidator dataAddressValidator) {
        this.transferProcessStore = transferProcessStore;
        this.manager = manager;
        this.transactionContext = transactionContext;
        this.negotiationStore = negotiationStore;
        this.contractValidationService = contractValidationService;
        this.dataAddressValidator = dataAddressValidator;
        queryValidator = new QueryValidator(TransferProcess.class, getSubtypes());
    }

    @Override
    public @Nullable TransferProcess findById(String transferProcessId) {
        return transactionContext.execute(() -> transferProcessStore.find(transferProcessId));
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
            var process = transferProcessStore.find(transferProcessId);
            return Optional.ofNullable(process).map(p -> TransferProcessStates.from(p.getState()).name()).orElse(null);
        });
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> cancel(String transferProcessId) {
        return apply(transferProcessId, this::cancelImpl);
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> complete(String transferProcessId) {
        return apply(transferProcessId, this::completeImpl);
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> fail(String transferProcessId, String errorDetail) {
        return apply(transferProcessId, failImpl(errorDetail));
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> deprovision(String transferProcessId) {
        return apply(transferProcessId, this::deprovisionImpl);
    }

    @Override
    public @NotNull ServiceResult<String> initiateTransfer(DataRequest request) {
        var validDestination = dataAddressValidator.validate(request.getDataDestination());
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
    public @NotNull ServiceResult<String> initiateTransfer(DataRequest request, ClaimToken claimToken) {
        var validDestination = dataAddressValidator.validate(request.getDataDestination());
        if (validDestination.failed()) {
            return ServiceResult.badRequest(validDestination.getFailureMessages().toArray(new String[]{}));
        }

        return transactionContext.execute(() ->
                Optional.ofNullable(negotiationStore.findContractAgreement(request.getContractId()))
                        .filter(agreement -> contractValidationService.validateAgreement(claimToken, agreement).succeeded())
                        .map(agreement -> manager.initiateProviderRequest(request))
                        .filter(AbstractResult::succeeded)
                        .map(AbstractResult::getContent)
                        .map(ServiceResult::success)
                        .orElse(ServiceResult.conflict("Request couldn't be initialised.")));
    }

    @Override
    public ServiceResult<TransferProcess> completeDeprovision(String transferProcessId, DeprovisionedResource resource) {
        return apply(transferProcessId, completeDeprovisionImpl(resource));
    }

    @Override
    public ServiceResult<TransferProcess> addProvisionedResource(String transferProcessId, ProvisionResponse response) {
        return apply(transferProcessId, addProvisionedResourceImpl(response));
    }

    private Map<Class<?>, List<Class<?>>> getSubtypes() {
        return Map.of(
                ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                ProvisionedDataAddressResource.class, List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)
        );
    }

    private ServiceResult<TransferProcess> apply(String transferProcessId, Function<TransferProcess, ServiceResult<TransferProcess>> function) {
        return transactionContext.execute(() -> {
            var transferProcess = transferProcessStore.find(transferProcessId);
            return Optional.ofNullable(transferProcess)
                    .map(function)
                    .orElse(ServiceResult.notFound(format("TransferProcess %s does not exist", transferProcessId)));
        });
    }

    private ServiceResult<TransferProcess> cancelImpl(TransferProcess transferProcess) {
        // Attempt the transition only to verify that the transition is allowed.
        // The updated transfer process is not persisted at this point, and is discarded.
        try {
            transferProcess.transitionCancelled();
        } catch (IllegalStateException e) {
            return ServiceResult.conflict(format("TransferProcess %s cannot be canceled as it is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
        }

        manager.enqueueCommand(new CancelTransferCommand(transferProcess.getId()));

        return ServiceResult.success(transferProcess);
    }

    private ServiceResult<TransferProcess> deprovisionImpl(TransferProcess transferProcess) {
        // Attempt the transition only to verify that the transition is allowed.
        // The updated transfer process is not persisted at this point, and is discarded.
        try {
            transferProcess.transitionDeprovisioning();
        } catch (IllegalStateException e) {
            return ServiceResult.conflict(format("TransferProcess %s cannot be deprovisioned as it is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
        }

        manager.enqueueCommand(new DeprovisionRequest(transferProcess.getId()));

        return ServiceResult.success(transferProcess);
    }

    private ServiceResult<TransferProcess> completeImpl(TransferProcess transferProcess) {
        // Attempt the transition only to verify that the transition is allowed.
        // The updated transfer process is not persisted at this point, and is discarded.
        try {
            transferProcess.transitionCompleted();
        } catch (IllegalStateException e) {
            return ServiceResult.conflict(format("TransferProcess %s cannot be completed as it is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
        }

        manager.enqueueCommand(new CompleteTransferCommand(transferProcess.getId()));

        return ServiceResult.success(transferProcess);
    }

    private Function<TransferProcess, ServiceResult<TransferProcess>> failImpl(String failReason) {

        return (transferProcess) -> {
            // Attempt the transition only to verify that the transition is allowed.
            // The updated transfer process is not persisted at this point, and is discarded.
            try {
                transferProcess.transitionError(failReason);
            } catch (IllegalStateException e) {
                return ServiceResult.conflict(format("Cannot fail TransferProcess %s as it is in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
            }

            manager.enqueueCommand(new FailTransferCommand(transferProcess.getId(), failReason));

            return ServiceResult.success(transferProcess);
        };

    }

    private Function<TransferProcess, ServiceResult<TransferProcess>> completeDeprovisionImpl(DeprovisionedResource resource) {
        return (transferProcess -> {
            manager.enqueueCommand(new DeprovisionCompleteCommand(transferProcess.getId(), resource));
            return ServiceResult.success(transferProcess);
        });
    }

    private Function<TransferProcess, ServiceResult<TransferProcess>> addProvisionedResourceImpl(ProvisionResponse response) {
        return (transferProcess -> {
            manager.enqueueCommand(new AddProvisionedResourceCommand(transferProcess.getId(), response));
            return ServiceResult.success(transferProcess);
        });
    }
}
