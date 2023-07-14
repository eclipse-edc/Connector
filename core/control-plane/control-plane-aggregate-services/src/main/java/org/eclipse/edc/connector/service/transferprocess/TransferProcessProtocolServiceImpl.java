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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;

public class TransferProcessProtocolServiceImpl implements TransferProcessProtocolService {

    private final TransferProcessStore transferProcessStore;
    private final TransactionContext transactionContext;
    private final ContractNegotiationStore negotiationStore;
    private final ContractValidationService contractValidationService;
    private final DataAddressValidator dataAddressValidator;
    private final TransferProcessObservable observable;
    private final Clock clock;
    private final Monitor monitor;
    private final Telemetry telemetry;

    public TransferProcessProtocolServiceImpl(TransferProcessStore transferProcessStore,
                                              TransactionContext transactionContext, ContractNegotiationStore negotiationStore,
                                              ContractValidationService contractValidationService,
                                              DataAddressValidator dataAddressValidator, TransferProcessObservable observable,
                                              Clock clock, Monitor monitor, Telemetry telemetry) {
        this.transferProcessStore = transferProcessStore;
        this.transactionContext = transactionContext;
        this.negotiationStore = negotiationStore;
        this.contractValidationService = contractValidationService;
        this.dataAddressValidator = dataAddressValidator;
        this.observable = observable;
        this.clock = clock;
        this.monitor = monitor;
        this.telemetry = telemetry;
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyRequested(TransferRequestMessage message, ClaimToken claimToken) {
        var contractIdResult = ContractId.parseId(message.getContractId());
        if (contractIdResult.failed()) {
            return ServiceResult.badRequest("ContractId is not valid: " + contractIdResult.getFailureDetail());
        }

        var validDestination = dataAddressValidator.validate(message.getDataDestination());
        if (validDestination.failed()) {
            return ServiceResult.badRequest(validDestination.getFailureMessages());
        }

        return transactionContext.execute(() ->
                Optional.ofNullable(negotiationStore.findContractAgreement(message.getContractId()))
                        .filter(agreement -> contractValidationService.validateAgreement(claimToken, agreement).succeeded())
                        .map(agreement -> requestedAction(message, contractIdResult.getContent()))
                        .orElse(ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "agreement not found or not valid"))));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyStarted(TransferStartMessage message, ClaimToken claimToken) {
        return onMessageDo(message, transferProcess -> startedAction(message, transferProcess));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyCompleted(TransferCompletionMessage message, ClaimToken claimToken) {
        return onMessageDo(message, transferProcess -> completedAction(message, transferProcess));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyTerminated(TransferTerminationMessage message, ClaimToken claimToken) {
        return onMessageDo(message, transferProcess -> terminatedAction(message, transferProcess));
    }
    
    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> findById(String id, ClaimToken claimToken) {
        return transactionContext.execute(() -> Optional.ofNullable(transferProcessStore.findById(id))
                .filter(tp -> validateCounterParty(claimToken, tp))
                .map(ServiceResult::success)
                .orElse(ServiceResult.notFound(format("No negotiation with id %s found", id))));
    }
    
    @NotNull
    private ServiceResult<TransferProcess> requestedAction(TransferRequestMessage message, ContractId contractId) {
        var assetId = contractId.assetIdPart();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(message.getProcessId())
                .protocol(message.getProtocol())
                .connectorAddress(message.getCallbackAddress())
                .dataDestination(message.getDataDestination())
                .assetId(assetId)
                .contractId(message.getContractId())
                .build();

        var existingTransferProcess = transferProcessStore.findForCorrelationId(dataRequest.getId());
        if (existingTransferProcess != null) {
            return ServiceResult.success(existingTransferProcess);
        }
        var process = TransferProcess.Builder.newInstance()
                .id(randomUUID().toString())
                .dataRequest(dataRequest)
                .type(PROVIDER)
                .clock(clock)
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        observable.invokeForEach(l -> l.preCreated(process));
        update(process);
        observable.invokeForEach(l -> l.initiated(process));

        return ServiceResult.success(process);
    }

    @NotNull
    private ServiceResult<TransferProcess> startedAction(TransferStartMessage message, TransferProcess transferProcess) {
        if (transferProcess.getType() == CONSUMER && transferProcess.canBeStartedConsumer()) {
            observable.invokeForEach(l -> l.preStarted(transferProcess));
            transferProcess.transitionStarted();
            update(transferProcess);
            var transferStartedData = TransferProcessStartedData.Builder.newInstance()
                    .dataAddress(message.getDataAddress())
                    .build();
            observable.invokeForEach(l -> l.started(transferProcess, transferStartedData));
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be started"));
        }
    }

    @NotNull
    private ServiceResult<TransferProcess> completedAction(TransferCompletionMessage message, TransferProcess transferProcess) {
        if (transferProcess.canBeCompleted()) {
            observable.invokeForEach(l -> l.preCompleted(transferProcess));
            transferProcess.transitionCompleted();
            update(transferProcess);
            observable.invokeForEach(l -> l.completed(transferProcess));
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be completed"));
        }
    }

    @NotNull
    private ServiceResult<TransferProcess> terminatedAction(TransferTerminationMessage message, TransferProcess transferProcess) {
        if (transferProcess.canBeTerminated()) {
            observable.invokeForEach(l -> l.preTerminated(transferProcess));
            transferProcess.transitionTerminated();
            update(transferProcess);
            observable.invokeForEach(l -> l.terminated(transferProcess));
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be terminated"));
        }
    }

    private ServiceResult<TransferProcess> onMessageDo(TransferRemoteMessage message, Function<TransferProcess, ServiceResult<TransferProcess>> action) {
        return transactionContext.execute(() -> Optional.of(message.getProcessId())
                .map(transferProcessStore::findForCorrelationId)
                .map(action)
                .orElse(ServiceResult.notFound(format("TransferProcess with DataRequest id %s not found", message.getProcessId()))));
    }
    
    private boolean validateCounterParty(ClaimToken claimToken, TransferProcess transferProcess) {
        return Optional.ofNullable(negotiationStore.findContractAgreement(transferProcess.getContractId()))
                .map(agreement -> contractValidationService.validateRequest(claimToken, agreement))
                .filter(Result::succeeded)
                .isPresent();
    }

    private void update(TransferProcess transferProcess) {
        transferProcessStore.updateOrCreate(transferProcess);
        monitor.debug(format("TransferProcess %s is now in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
    }

}
