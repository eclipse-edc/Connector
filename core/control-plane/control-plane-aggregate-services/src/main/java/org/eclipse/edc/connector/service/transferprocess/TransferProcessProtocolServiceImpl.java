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
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.service.protocol.BaseProtocolService;
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
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;

public class TransferProcessProtocolServiceImpl extends BaseProtocolService implements TransferProcessProtocolService {

    @PolicyScope
    private static final String TRANSFER_PROCESS_REQUEST_SCOPE = "request.transfer.process";
    private final TransferProcessStore transferProcessStore;
    private final TransactionContext transactionContext;
    private final ContractNegotiationStore negotiationStore;
    private final ContractValidationService contractValidationService;
    private final DataAddressValidatorRegistry dataAddressValidator;
    private final TransferProcessObservable observable;
    private final Clock clock;
    private final Monitor monitor;
    private final Telemetry telemetry;

    public TransferProcessProtocolServiceImpl(TransferProcessStore transferProcessStore,
                                              TransactionContext transactionContext, ContractNegotiationStore negotiationStore,
                                              ContractValidationService contractValidationService,
                                              IdentityService identityService,
                                              PolicyEngine policyEngine,
                                              DataAddressValidatorRegistry dataAddressValidator, TransferProcessObservable observable,
                                              Clock clock, Monitor monitor, Telemetry telemetry) {
        super(identityService, policyEngine, monitor);
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
    public ServiceResult<TransferProcess> notifyRequested(TransferRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchNotifyRequestContext(message)
                .compose(context -> verifyRequest(tokenRepresentation, context))
                .compose(context -> validateDestination(message, context))
                .compose(context -> validateAgreement(message, context))
                .compose(context -> requestedAction(message, context.agreement().getAssetId())));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyStarted(TransferStartMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(message, this::findTransferProcess)
                .compose(context -> verifyRequest(tokenRepresentation, context))
                .compose(context -> onMessageDo(message, context.claimToken(), context.agreement(), transferProcess -> startedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyCompleted(TransferCompletionMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(message, this::findTransferProcess)
                .compose(context -> verifyRequest(tokenRepresentation, context))
                .compose(context -> onMessageDo(message, context.claimToken(), context.agreement(), transferProcess -> completedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyTerminated(TransferTerminationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(message, this::findTransferProcess)
                .compose(context -> verifyRequest(tokenRepresentation, context))
                .compose(context -> onMessageDo(message, context.claimToken(), context.agreement(), transferProcess -> terminatedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> findById(String id, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(id, this::findTransferProcessById)
                .compose(context -> verifyRequest(tokenRepresentation, context))
                .compose(context -> validateCounterParty(context.claimToken(), context.agreement(), context.transferProcess())));
    }

    @NotNull
    private ServiceResult<TransferProcess> requestedAction(TransferRequestMessage message, String assetId) {
        var destination = message.getDataDestination() != null
                ? message.getDataDestination() : DataAddress.Builder.newInstance().type(HTTP_PROXY).build();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(message.getConsumerPid())
                .protocol(message.getProtocol())
                .connectorAddress(message.getCallbackAddress())
                .dataDestination(destination)
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
                .transferType(message.getTransferType())
                .type(PROVIDER)
                .clock(clock)
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        observable.invokeForEach(l -> l.preCreated(process));
        process.protocolMessageReceived(message.getId());
        update(process);
        observable.invokeForEach(l -> l.initiated(process));

        return ServiceResult.success(process);
    }

    @NotNull
    private ServiceResult<TransferProcess> startedAction(TransferStartMessage message, TransferProcess transferProcess) {
        if (transferProcess.getType() == CONSUMER && transferProcess.canBeStartedConsumer()) {
            observable.invokeForEach(l -> l.preStarted(transferProcess));
            transferProcess.protocolMessageReceived(message.getId());
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
            transferProcess.protocolMessageReceived(message.getId());
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
            transferProcess.protocolMessageReceived(message.getId());
            update(transferProcess);
            observable.invokeForEach(l -> l.terminated(transferProcess));
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be terminated"));
        }
    }

    private ServiceResult<ClaimTokenContext> validateDestination(TransferRequestMessage message, ClaimTokenContext context) {
        var destination = message.getDataDestination();
        if (destination != null) {
            var validDestination = dataAddressValidator.validateDestination(destination);
            if (validDestination.failed()) {
                return ServiceResult.badRequest(validDestination.getFailureMessages());
            }
        }
        return ServiceResult.success(context);
    }

    private ServiceResult<ClaimTokenContext> validateAgreement(TransferRemoteMessage message, ClaimTokenContext context) {
        var validationResult = contractValidationService.validateAgreement(context.claimToken(), context.agreement());
        if (validationResult.failed()) {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "agreement not found or not valid"));
        }
        return ServiceResult.success(context);
    }

    private ServiceResult<TransferRequestMessageContext> fetchNotifyRequestContext(TransferRequestMessage message) {
        return Optional.ofNullable(negotiationStore.findContractAgreement(message.getContractId()))
                .map(contractAgreement -> new TransferRequestMessageContext(contractAgreement, null))
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.notFound(format("Cannot process %s because %s", message.getClass().getSimpleName(), "agreement not found or not valid")));
    }

    private <T> ServiceResult<TransferRequestMessageContext> fetchRequestContext(T input, Function<T, ServiceResult<TransferProcess>> tpProvider) {
        return tpProvider.apply(input).compose(transferProcess -> findContractByTransferProcess(transferProcess).map(agreement -> new TransferRequestMessageContext(agreement, transferProcess)));
    }

    private ServiceResult<ClaimTokenContext> verifyRequest(TokenRepresentation tokenRepresentation, TransferRequestMessageContext context) {
        var result = verifyToken(tokenRepresentation, TRANSFER_PROCESS_REQUEST_SCOPE, context.agreement().getPolicy());
        if (result.failed()) {
            monitor.debug(() -> "Verification Failed: %s".formatted(result.getFailureDetail()));
            return ServiceResult.notFound("Not found");
        } else {
            return ServiceResult.success(new ClaimTokenContext(result.getContent(), context.agreement(), context.transferProcess()));
        }
    }

    private ServiceResult<ContractAgreement> findContractByTransferProcess(TransferProcess transferProcess) {
        var agreement = negotiationStore.findContractAgreement(transferProcess.getContractId());
        if (agreement == null) {
            return ServiceResult.notFound(format("No transfer process with id %s found", transferProcess.getId()));
        }
        return ServiceResult.success(agreement);
    }

    private ServiceResult<TransferProcess> onMessageDo(TransferRemoteMessage message, ClaimToken claimToken, ContractAgreement agreement, Function<TransferProcess, ServiceResult<TransferProcess>> action) {
        return findAndLease(message)
                .compose(transferProcess -> validateCounterParty(claimToken, agreement, transferProcess)
                        .compose(p -> {
                            if (p.shouldIgnoreIncomingMessage(message.getId())) {
                                return ServiceResult.success(p);
                            } else {
                                return action.apply(p);
                            }
                        })
                        .onFailure(f -> breakLease(transferProcess)));
    }

    private ServiceResult<TransferProcess> validateCounterParty(ClaimToken claimToken, ContractAgreement agreement, TransferProcess transferProcess) {
        var validation = contractValidationService.validateRequest(claimToken, agreement);
        if (validation.failed()) {
            return ServiceResult.badRequest(validation.getFailureMessages());
        }

        return ServiceResult.success(transferProcess);
    }

    // find and lease - write access
    private ServiceResult<TransferProcess> findAndLease(TransferRemoteMessage remoteMessage) {
        return transferProcessStore
                .findByIdAndLease(remoteMessage.getProcessId())
                // recover needed to maintain backward compatibility when there was no distinction between providerPid and consumerPid
                .recover(it -> transferProcessStore.findByCorrelationIdAndLease(remoteMessage.getProcessId()))
                .flatMap(ServiceResult::from);
    }

    // read only access
    private ServiceResult<TransferProcess> findTransferProcess(TransferRemoteMessage remoteMessage) {
        return findTransferProcessById(remoteMessage.getProcessId());
    }

    // read only access
    private ServiceResult<TransferProcess> findTransferProcessById(String id) {
        return Optional.ofNullable(transferProcessStore.findById(id))
                // or needed to maintain backward compatibility when there was no distinction between providerPid and consumerPid
                .or(() -> Optional.ofNullable(transferProcessStore.findForCorrelationId(id)))
                .map(ServiceResult::success)
                .orElseGet(() -> notFound(id));
    }

    private ServiceResult<TransferProcess> notFound(String transferProcessId) {
        return ServiceResult.notFound(format("No transfer process with id %s found", transferProcessId));
    }

    private void breakLease(TransferProcess process) {
        transferProcessStore.save(process);
    }

    private void update(TransferProcess transferProcess) {
        transferProcessStore.save(transferProcess);
        monitor.debug(format("TransferProcess %s is now in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
    }

    private record TransferRequestMessageContext(ContractAgreement agreement, TransferProcess transferProcess) {
    }

    private record ClaimTokenContext(ClaimToken claimToken, ContractAgreement agreement,
                                     TransferProcess transferProcess) {
    }
}
