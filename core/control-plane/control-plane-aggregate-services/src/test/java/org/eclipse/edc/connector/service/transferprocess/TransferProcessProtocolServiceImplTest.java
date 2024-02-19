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
import org.eclipse.edc.connector.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
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
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.service.transferprocess.TransferProcessProtocolServiceImpl.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessProtocolServiceImplTest {

    private final TransferProcessStore store = mock();
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final ContractNegotiationStore negotiationStore = mock();
    private final ContractValidationService validationService = mock();
    private final DataAddressValidatorRegistry dataAddressValidator = mock();
    private final TransferProcessListener listener = mock();

    private final ProtocolTokenValidator protocolTokenValidator = mock();

    private TransferProcessProtocolService service;

    @BeforeEach
    void setUp() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        service = new TransferProcessProtocolServiceImpl(store, transactionContext, negotiationStore, validationService,
                protocolTokenValidator, dataAddressValidator, observable, mock(), mock(), mock());

    }

    @Test
    void notifyRequested_validAgreement_shouldInitiateTransfer() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .processId("consumerPid")
                .contractId("agreementId")
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().satisfies(tp -> {
            assertThat(tp.getCorrelationId()).isEqualTo("consumerPid");
            assertThat(tp.getConnectorAddress()).isEqualTo("http://any");
            assertThat(tp.getAssetId()).isEqualTo("assetId");
        });
        verify(listener).preCreated(any());
        verify(store).save(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).initiated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_doNothingIfProcessAlreadyExist() {
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("correlationId")
                .consumerPid("consumerPid")
                .contractId("agreementId")
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());
        when(store.findForCorrelationId(any())).thenReturn(transferProcess(REQUESTED, "transferProcessId"));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().extracting(TransferProcess::getId).isEqualTo("transferProcessId");
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidAgreement_shouldNotInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .protocol("protocol")
                .callbackAddress("http://any")
                .contractId("agreementId")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenReturn(Result.failure("error"));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.success());

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidDestination_shouldNotInitiateTransfer() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .protocol("protocol")
                .contractId("agreementId")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(dataAddressValidator.validateDestination(any())).thenReturn(ValidationResult.failure(violation("invalid data address", "path")));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_missingDestination_shouldInitiateTransfer() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferRequestMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .processId("consumerPid")
                .protocol("protocol")
                .contractId("agreementId")
                .callbackAddress("http://any")
                .build();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenReturn(Result.success(null));

        var result = service.notifyRequested(message, tokenRepresentation);

        assertThat(result).isSucceeded().satisfies(transferProcess -> {
            assertThat(transferProcess.getCorrelationId()).isEqualTo("consumerPid");
            assertThat(transferProcess.getConnectorAddress()).isEqualTo("http://any");
            assertThat(transferProcess.getAssetId()).isEqualTo("assetId");
            assertThat(transferProcess.getDataDestination().getType()).isEqualTo(HTTP_PROXY);
        });
        verify(listener).preCreated(any());
        verify(store).save(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).initiated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyStarted_shouldTransitionToStarted() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
        var agreement = contractAgreement();
        var transferProcess = transferProcess(STARTED, "transferProcessId");

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.notifyStarted(message, tokenRepresentation);

        var startedDataCaptor = ArgumentCaptor.forClass(TransferProcessStartedData.class);
        var transferProcessCaptor = ArgumentCaptor.forClass(TransferProcess.class);
        assertThat(result).isSucceeded();
        verify(listener).preStarted(any());
        verify(store).save(transferProcessCaptor.capture());
        verify(store).save(argThat(t -> t.getState() == STARTED.code()));
        verify(listener).started(any(), startedDataCaptor.capture());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
        assertThat(startedDataCaptor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(message.getDataAddress());
    }

    @Test
    void notifyStarted_shouldReturnConflict_whenTransferCannotBeStarted() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(DEPROVISIONING, UUID.randomUUID().toString());
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.notifyStarted(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == DEPROVISIONING.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyStarted_shouldReturnBadRequest_whenCounterPartyUnauthorized() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
        var agreement = contractAgreement();

        var transferProcess = transferProcess(REQUESTED, "transferProcessId");
        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyStarted(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(BAD_REQUEST);

        verify(store, times(1)).save(any());

    }

    @Test
    void notifyCompleted_shouldTransitionToCompleted() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();
        var transferProcess = transferProcess(STARTED, "transferProcessId");

        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(listener).preCompleted(any());
        verify(store).save(argThat(t -> t.getState() == COMPLETED.code()));
        verify(listener).completed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyCompleted_shouldReturnConflict_whenStatusIsNotValid() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(REQUESTED, UUID.randomUUID().toString());
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();
        var agreement = contractAgreement();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == REQUESTED.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyCompleted_shouldReturnBadRequest_whenCounterPartyUnauthorized() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();

        var agreement = contractAgreement();

        var transferProcess = transferProcess(STARTED, "transferProcessId");
        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyCompleted(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(BAD_REQUEST);

        verify(store, times(1)).save(any());

    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();
        var agreement = contractAgreement();
        var transferProcess = transferProcess(STARTED, "transferProcessId");

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());
        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result).isSucceeded();
        verify(listener).preTerminated(any());
        verify(store).save(argThat(t -> t.getState() == TERMINATED.code()));
        verify(listener).terminated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldReturnConflict_whenTransferProcessCannotBeTerminated() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var transferProcess = transferProcess(DEPROVISIONING, UUID.randomUUID().toString());
        var agreement = contractAgreement();
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        // state didn't change
        verify(store, times(1)).save(argThat(tp -> tp.getState() == DEPROVISIONING.code()));
        verifyNoInteractions(listener);
    }

    @Test
    void notifyTerminated_shouldReturnBadRequest_whenCounterPartyUnauthorized() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var agreement = contractAgreement();
        var transferProcess = transferProcess(TERMINATED, UUID.randomUUID().toString());
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById("correlationId")).thenReturn(transferProcess);
        when(store.findByIdAndLease("correlationId")).thenReturn(StoreResult.success(transferProcess));
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.failure("error"));

        var result = service.notifyTerminated(message, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(BAD_REQUEST);

        verify(store, times(1)).save(any());

    }

    @Test
    void findById_shouldReturnTransferProcess_whenValidCounterParty() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var agreement = contractAgreement();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.success());

        var result = service.findById(processId, tokenRepresentation);

        assertThat(result)
                .isSucceeded()
                .isEqualTo(transferProcess);
    }

    @Test
    void findById_shouldReturnNotFound_whenNegotiationNotFound() {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById(any())).thenReturn(null);

        var result = service.findById("invalidId", tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void findById_shouldReturnBadRequest_whenCounterPartyUnauthorized() {
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();
        var agreement = contractAgreement();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(participantAgent, agreement)).thenReturn(Result.failure("error"));

        var result = service.findById(processId, tokenRepresentation);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(BAD_REQUEST);
    }

    @ParameterizedTest
    @ArgumentsSource(NotifyArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTransferProcessNotFound(MethodCall<M> methodCall, M message) {
        var participantAgent = participantAgent();
        var tokenRepresentation = tokenRepresentation();

        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent));
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.notFound("not found"));
        when(store.findByCorrelationIdAndLease(any())).thenReturn(StoreResult.notFound("not found"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).matches(ServiceResult::failed);
        verify(store, never()).save(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @ParameterizedTest
    @ArgumentsSource(NotifyArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTokenValidationFails(MethodCall<M> methodCall, M message) {
        var tokenRepresentation = tokenRepresentation();

        when(store.findById(any())).thenReturn(transferProcessBuilder().build());
        when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(transferProcessBuilder().build()));
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(protocolTokenValidator.verify(eq(tokenRepresentation), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));

        var result = methodCall.call(service, message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    private TransferProcess transferProcess(TransferProcessStates state, String id) {
        return transferProcessBuilder()
                .id(id)
                .state(state.code())
                .build();
    }

    private TransferProcess.Builder transferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .dataRequest(dataRequest());
    }

    private ParticipantAgent participantAgent() {
        return new ParticipantAgent(emptyMap(), emptyMap());
    }

    private TokenRepresentation tokenRepresentation() {
        return TokenRepresentation.Builder.newInstance()
                .token(UUID.randomUUID().toString())
                .build();
    }

    private ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerId("provider")
                .consumerId("consumer")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private DataRequest dataRequest() {
        return DataRequest.Builder.newInstance()
                .contractId("contractId")
                .destinationType("type")
                .build();
    }

    @FunctionalInterface
    private interface MethodCall<M extends RemoteMessage> {
        ServiceResult<?> call(TransferProcessProtocolService service, M message, TokenRepresentation token);
    }

    private static class NotifyArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            MethodCall<TransferStartMessage> started = TransferProcessProtocolService::notifyStarted;
            MethodCall<TransferCompletionMessage> completed = TransferProcessProtocolService::notifyCompleted;
            MethodCall<TransferTerminationMessage> terminated = TransferProcessProtocolService::notifyTerminated;
            return Stream.of(
                    arguments(started,
                            build(TransferStartMessage.Builder.newInstance()),
                            CONSUMER, REQUESTED
                    ),
                    arguments(completed,
                            build(TransferCompletionMessage.Builder.newInstance()),
                            CONSUMER, STARTED
                    ),
                    arguments(terminated,
                            build(TransferTerminationMessage.Builder.newInstance().code("TestCode").reason("TestReason")),
                            PROVIDER, STARTED
                    )
            );
        }

        private <M extends TransferRemoteMessage> M build(TransferRemoteMessage.Builder<M, ?> builder) {
            return builder.protocol("protocol").counterPartyAddress("http://any").processId("correlationId").build();
        }
    }

    @Nested
    class IdempotencyProcessStateReplication {

        @ParameterizedTest
        @ArgumentsSource(NotifyArguments.class)
        <M extends ProcessRemoteMessage> void notify_shouldStoreReceivedMessageId(MethodCall<M> methodCall, M message,
                                                                                  TransferProcess.Type type,
                                                                                  TransferProcessStates currentState) {
            var transferProcess = transferProcessBuilder().state(currentState.code()).type(type).build();
            when(protocolTokenValidator.verify(any(), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent()));
            when(store.findById(any())).thenReturn(transferProcess);
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(transferProcess));
            when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
            when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenAnswer(i -> Result.success(i.getArgument(1)));
            when(validationService.validateRequest(any(ParticipantAgent.class), isA(ContractAgreement.class))).thenReturn(Result.success());

            var result = methodCall.call(service, message, tokenRepresentation());

            assertThat(result).isSucceeded();
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(store).save(captor.capture());
            var storedTransferProcess = captor.getValue();
            assertThat(storedTransferProcess.getProtocolMessages().isAlreadyReceived(message.getId())).isTrue();
        }

        @ParameterizedTest
        @ArgumentsSource(NotifyArguments.class)
        <M extends ProcessRemoteMessage> void notify_shouldIgnoreMessage_whenAlreadyReceived(MethodCall<M> methodCall, M message,
                                                                                             TransferProcess.Type type,
                                                                                             TransferProcessStates currentState) {
            var transferProcess = transferProcessBuilder().state(currentState.code()).type(type).build();
            transferProcess.protocolMessageReceived(message.getId());
            when(protocolTokenValidator.verify(any(), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent()));
            when(store.findById(any())).thenReturn(transferProcess);
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(transferProcess));
            when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
            when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenAnswer(i -> Result.success(i.getArgument(1)));
            when(validationService.validateRequest(any(ParticipantAgent.class), isA(ContractAgreement.class))).thenReturn(Result.success());

            var result = methodCall.call(service, message, tokenRepresentation());

            assertThat(result).isSucceeded();
            verify(store, never()).save(any());
            verifyNoInteractions(listener);
        }

        @ParameterizedTest
        @ArgumentsSource(NotifyArguments.class)
        <M extends ProcessRemoteMessage> void notify_shouldIgnoreMessage_whenFinalState(MethodCall<M> methodCall, M message,
                                                                                        TransferProcess.Type type) {
            var transferProcess = transferProcessBuilder().state(COMPLETED.code()).type(type).build();
            when(protocolTokenValidator.verify(any(), eq(TRANSFER_PROCESS_REQUEST_SCOPE), any())).thenReturn(ServiceResult.success(participantAgent()));
            when(store.findById(any())).thenReturn(transferProcess);
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(transferProcess));
            when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
            when(validationService.validateAgreement(any(ParticipantAgent.class), any())).thenAnswer(i -> Result.success(i.getArgument(1)));
            when(validationService.validateRequest(any(ParticipantAgent.class), isA(ContractAgreement.class))).thenReturn(Result.success());

            var result = methodCall.call(service, message, tokenRepresentation());

            assertThat(result).isSucceeded();
            verify(store, never()).save(any());
            verifyNoInteractions(listener);
        }
    }
}
