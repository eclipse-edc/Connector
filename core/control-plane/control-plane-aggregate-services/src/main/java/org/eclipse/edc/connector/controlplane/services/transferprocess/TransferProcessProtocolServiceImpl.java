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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

public class TransferProcessProtocolServiceImpl implements TransferProcessProtocolService {

    private final TransferProcessStore transferProcessStore;
    private final TransactionContext transactionContext;
    private final ContractNegotiationStore negotiationStore;
    private final ContractValidationService contractValidationService;
    private final DataAddressValidatorRegistry dataAddressValidator;
    private final TransferProcessObservable observable;
    private final ProtocolTokenValidator protocolTokenValidator;
    private final Clock clock;
    private final Monitor monitor;
    private final Telemetry telemetry;
    private final Vault vault;

    public TransferProcessProtocolServiceImpl(TransferProcessStore transferProcessStore,
                                              TransactionContext transactionContext, ContractNegotiationStore negotiationStore,
                                              ContractValidationService contractValidationService,
                                              ProtocolTokenValidator protocolTokenValidator,
                                              DataAddressValidatorRegistry dataAddressValidator, TransferProcessObservable observable,
                                              Clock clock, Monitor monitor, Telemetry telemetry, Vault vault) {
        this.transferProcessStore = transferProcessStore;
        this.transactionContext = transactionContext;
        this.negotiationStore = negotiationStore;
        this.contractValidationService = contractValidationService;
        this.protocolTokenValidator = protocolTokenValidator;
        this.dataAddressValidator = dataAddressValidator;
        this.observable = observable;
        this.clock = clock;
        this.monitor = monitor;
        this.telemetry = telemetry;
        this.vault = vault;
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyRequested(ParticipantContext participantContext, TransferRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchNotifyRequestContext(participantContext, message)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> validateDestination(message, context))
                .compose(context -> validateAgreement(message, context))
                .compose(context -> requestedAction(participantContext, message, context.agreement())));
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyStarted(ParticipantContext participantContext, TransferStartMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(participantContext, message, this::findTransferProcess)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> onMessageDo(participantContext, message, context.participantAgent(), context.agreement(), transferProcess -> startedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyCompleted(ParticipantContext participantContext, TransferCompletionMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(participantContext, message, this::findTransferProcess)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> onMessageDo(participantContext, message, context.participantAgent(), context.agreement(), transferProcess -> completedAction(message, transferProcess)))
        );
    }

    @Override
    public @NotNull ServiceResult<TransferProcess> notifySuspended(ParticipantContext participantContext, TransferSuspensionMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(participantContext, message, this::findTransferProcess)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> onMessageDo(participantContext, message, context.participantAgent(), context.agreement(), transferProcess -> suspendedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> notifyTerminated(ParticipantContext participantContext, TransferTerminationMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> fetchRequestContext(participantContext, message, this::findTransferProcess)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> onMessageDo(participantContext, message, context.participantAgent(), context.agreement(), transferProcess -> terminatedAction(message, transferProcess)))
        );
    }

    @Override
    @WithSpan
    @NotNull
    public ServiceResult<TransferProcess> findById(ParticipantContext participantContext, String id, TokenRepresentation tokenRepresentation, String protocol) {
        var message = TransferProcessRequestMessage.Builder.newInstance()
                .transferProcessId(id)
                .protocol(protocol)
                .build();

        return transactionContext.execute(() -> fetchRequestContext(participantContext, id, this::findTransferProcessById)
                .compose(context -> verifyRequest(participantContext, tokenRepresentation, context, message))
                .compose(context -> validateCounterParty(context.participantAgent(), context.agreement(), context.transferProcess())));
    }

    @NotNull
    private ServiceResult<TransferProcess> requestedAction(ParticipantContext participantContext, TransferRequestMessage message, ContractAgreement contractAgreement) {
        var existingTransferProcess = transferProcessStore.findForCorrelationId(message.getConsumerPid());
        if (existingTransferProcess != null) {
            return ServiceResult.success(existingTransferProcess);
        }

        var id = randomUUID().toString();

        return offloadEventualSecretToVault(id, participantContext, message.getDataDestination())
                .map(destination -> {
                    var process = TransferProcess.Builder.newInstance()
                            .id(id)
                            .protocol(message.getProtocol())
                            .correlationId(message.getConsumerPid())
                            .counterPartyAddress(message.getCallbackAddress())
                            .dataDestination(destination)
                            .assetId(contractAgreement.getAssetId())
                            .contractId(contractAgreement.getId())
                            .transferType(message.getTransferType())
                            .type(PROVIDER)
                            .clock(clock)
                            .traceContext(telemetry.getCurrentTraceContext())
                            .participantContextId(participantContext.getParticipantContextId())
                            .build();

                    observable.invokeForEach(l -> l.preCreated(process));
                    process.protocolMessageReceived(message.getId());
                    update(process);
                    observable.invokeForEach(l -> l.initiated(process));

                    return process;
                });
    }

    private ServiceResult<DataAddress> offloadEventualSecretToVault(String id, ParticipantContext participantContext, DataAddress destination) {
        if (destination == null) {
            return ServiceResult.success(null);
        }

        var secret = destination.getStringProperty(DataAddress.EDC_DATA_ADDRESS_SECRET);
        if (secret == null) {
            return ServiceResult.success(destination);
        }

        var keyName = "transfer-process-" + id + "-destination-secret";

        var storeSecret = vault.storeSecret(participantContext.getParticipantContextId(), keyName, secret);
        if (storeSecret.failed()) {
            return ServiceResult.unexpected("cannot store destination secret: ", storeSecret.getFailureDetail());
        }

        var newDestination = destination.toBuilder()
                .keyName(keyName)
                .property(DataAddress.EDC_DATA_ADDRESS_SECRET, null)
                .build();

        return ServiceResult.success(newDestination);
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
        } else if (transferProcess.getType() == PROVIDER && transferProcess.currentStateIsOneOf(SUSPENDED)) {
            transferProcess.protocolMessageReceived(message.getId());
            transferProcess.transitionStarting();
            update(transferProcess);
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
            transferProcess.transitionCompletingRequested();
            update(transferProcess);
            observable.invokeForEach(l -> l.completed(transferProcess));
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be completed"));
        }
    }

    @NotNull
    private ServiceResult<TransferProcess> suspendedAction(TransferSuspensionMessage message, TransferProcess transferProcess) {
        if (transferProcess.canBeSuspended()) {
            var reason = message.getReason().stream().map(Object::toString).collect(joining(", "));
            transferProcess.transitionSuspendingRequested(reason);
            transferProcess.protocolMessageReceived(message.getId());
            update(transferProcess);
            return ServiceResult.success(transferProcess);
        } else {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "transfer cannot be suspended"));
        }
    }

    @NotNull
    private ServiceResult<TransferProcess> terminatedAction(TransferTerminationMessage message, TransferProcess transferProcess) {
        if (transferProcess.canBeTerminated()) {
            transferProcess.transitionTerminatingRequested();
            transferProcess.protocolMessageReceived(message.getId());
            update(transferProcess);
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
        var validationResult = contractValidationService.validateAgreement(context.participantAgent(), context.agreement());
        if (validationResult.failed()) {
            return ServiceResult.conflict(format("Cannot process %s because %s", message.getClass().getSimpleName(), "agreement not found or not valid"));
        }
        return ServiceResult.success(context);
    }

    private ServiceResult<TransferRequestMessageContext> fetchNotifyRequestContext(ParticipantContext participantContext, TransferRequestMessage message) {
        return Optional.ofNullable(findAgreement(participantContext, message.getContractId()))
                .filter(agreement -> participantContext.getParticipantContextId().equals(agreement.getParticipantContextId()))
                .map(contractAgreement -> new TransferRequestMessageContext(contractAgreement, null))
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.notFound(format("Cannot process %s because %s", message.getClass().getSimpleName(), "agreement not found or not valid")));
    }

    private <T> ServiceResult<TransferRequestMessageContext> fetchRequestContext(ParticipantContext participantContext, T input, BiFunction<ParticipantContext, T, ServiceResult<TransferProcess>> tpProvider) {
        return tpProvider.apply(participantContext, input).compose(transferProcess -> findContractByTransferProcess(transferProcess).map(agreement -> new TransferRequestMessageContext(agreement, transferProcess)));
    }

    private ServiceResult<ClaimTokenContext> verifyRequest(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, TransferRequestMessageContext context, RemoteMessage message) {
        var result = protocolTokenValidator.verify(participantContext, tokenRepresentation, RequestTransferProcessPolicyContext::new, context.agreement().getPolicy(), message);
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

    private ServiceResult<TransferProcess> onMessageDo(ParticipantContext participantContext, TransferRemoteMessage message,
                                                       ParticipantAgent participantAgent, ContractAgreement agreement,
                                                       Function<TransferProcess, ServiceResult<TransferProcess>> action) {
        return findAndLease(participantContext, message)
                .compose(transferProcess -> validateCounterParty(participantAgent, agreement, transferProcess)
                        .compose(p -> {
                            if (p.shouldIgnoreIncomingMessage(message.getId())) {
                                return ServiceResult.success(p);
                            } else {
                                return action.apply(p);
                            }
                        })
                        .onFailure(f -> breakLease(transferProcess)));
    }

    private ServiceResult<TransferProcess> validateCounterParty(ParticipantAgent participantAgent, ContractAgreement agreement, TransferProcess transferProcess) {
        var validation = contractValidationService.validateRequest(participantAgent, agreement);
        if (validation.failed()) {
            return ServiceResult.badRequest(validation.getFailureMessages());
        }

        return ServiceResult.success(transferProcess);
    }

    // find and lease - write access
    private ServiceResult<TransferProcess> findAndLease(ParticipantContext participantContext, TransferRemoteMessage remoteMessage) {
        return transferProcessStore
                .findByIdAndLease(remoteMessage.getProcessId())
                .flatMap(ServiceResult::from)
                .compose(tp -> filterByParticipantContext(participantContext, tp));
    }

    private ServiceResult<TransferProcess> filterByParticipantContext(ParticipantContext participantContext, TransferProcess transferProcess) {
        if (participantContext.getParticipantContextId().equals(transferProcess.getParticipantContextId())) {
            return ServiceResult.success(transferProcess);
        } else {
            return notFound(transferProcess.getId());
        }
    }

    // read only access
    private ServiceResult<TransferProcess> findTransferProcess(ParticipantContext participantContext, TransferRemoteMessage remoteMessage) {
        return findTransferProcessById(participantContext, remoteMessage.getProcessId());
    }

    private ContractAgreement findAgreement(ParticipantContext participantContext, String contractId) {
        var query = queryByParticipantContextId(participantContext.getParticipantContextId())
                .filter(Criterion.criterion("agreementId", "=", contractId))
                .build();
        try (var stream = negotiationStore.queryAgreements(query)) {
            return stream.findFirst().orElse(null);
        }
    }

    // read only access
    private ServiceResult<TransferProcess> findTransferProcessById(ParticipantContext participantContext, String id) {
        return Optional.ofNullable(transferProcessStore.findById(id))
                // or needed to maintain backward compatibility when there was no distinction between providerPid and consumerPid
                .or(() -> Optional.ofNullable(transferProcessStore.findForCorrelationId(id)))
                .filter(tp -> participantContext.getParticipantContextId().equals(tp.getParticipantContextId()))
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

    private record ClaimTokenContext(ParticipantAgent participantAgent, ContractAgreement agreement,
                                     TransferProcess transferProcess) {
    }
}
