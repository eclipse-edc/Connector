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

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessProtocolServiceImplTest {

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final ContractNegotiationStore negotiationStore = mock(ContractNegotiationStore.class);
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final DataAddressValidator dataAddressValidator = mock(DataAddressValidator.class);
    private final TransferProcessListener listener = mock(TransferProcessListener.class);

    private TransferProcessProtocolService service;

    @BeforeEach
    void setUp() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        service = new TransferProcessProtocolServiceImpl(store, transactionContext, negotiationStore, validationService,
                dataAddressValidator, observable, mock(Clock.class), mock(Monitor.class), mock(Telemetry.class));
    }

    @Test
    void notifyRequested_validAgreement_shouldInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("transferProcessId")
                .contractId(ContractId.create("definitionId", "assetId").toString())
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isSucceeded().satisfies(tp -> {
            assertThat(tp.getCorrelationId()).isEqualTo("transferProcessId");
            assertThat(tp.getConnectorAddress()).isEqualTo("http://any");
            assertThat(tp.getAssetId()).isEqualTo("assetId");
        });
        verify(listener).preCreated(any());
        verify(store).updateOrCreate(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).initiated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_doNothingIfProcessAlreadyExist() {
        var message = TransferRequestMessage.Builder.newInstance()
                .processId("correlationId")
                .contractId(ContractId.create("definitionId", "assetId").toString())
                .protocol("protocol")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess(REQUESTED, "transferProcessId"));

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isSucceeded().extracting(TransferProcess::getId).isEqualTo("transferProcessId");
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidContractId_shouldNotInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .contractId("notvalidcontractid")
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener, store, negotiationStore, validationService);
    }

    @Test
    void notifyRequested_invalidAgreement_shouldNotInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://any")
                .contractId(ContractId.create("definitionId", "assetId").toString())
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.failure("error"));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidDestination_shouldNotInitiateTransfer() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.failure("invalid data address"));
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .contractId(ContractId.create("definitionId", "assetId").toString())
                .callbackAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyStarted_shouldTransitionToStarted() {
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess(REQUESTED, "transferProcessId"));
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();

        var result = service.notifyStarted(message, claimToken());

        var captor = ArgumentCaptor.forClass(TransferProcessStartedData.class);

        assertThat(result).isSucceeded();
        verify(listener).preStarted(any());
        verify(store).updateOrCreate(argThat(t -> t.getState() == STARTED.code()));
        verify(listener).started(any(), captor.capture());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));

        assertThat(captor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(message.getDataAddress());
    }

    @Test
    void notifyStarted_shouldReturnConflict_whenStatusIsNotValid() {
        var transferProcess = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(COMPLETED.code()).build();
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess);
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();

        var result = service.notifyStarted(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyCompleted_shouldTransitionToCompleted() {
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess(STARTED, "transferProcessId"));
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();

        var result = service.notifyCompleted(message, claimToken());

        assertThat(result).isSucceeded();
        verify(listener).preCompleted(any());
        verify(store).updateOrCreate(argThat(t -> t.getState() == COMPLETED.code()));
        verify(listener).completed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyCompleted_shouldReturnConflict_whenStatusIsNotValid() {
        var transferProcess = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(REQUESTED.code()).build();
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess);
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .build();

        var result = service.notifyCompleted(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess(STARTED, "transferProcessId"));
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        var result = service.notifyTerminated(message, claimToken());

        assertThat(result).isSucceeded();
        verify(listener).preTerminated(any());
        verify(store).updateOrCreate(argThat(t -> t.getState() == TERMINATED.code()));
        verify(listener).terminated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldReturnConflict_whenStatusIsNotValid() {
        var transferProcess = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(TERMINATED.code()).build();
        when(store.findForCorrelationId("correlationId")).thenReturn(transferProcess);
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://any")
                .processId("correlationId")
                .code("TestCode")
                .reason("TestReason")
                .build();

        var result = service.notifyTerminated(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).updateOrCreate(any());
        verifyNoInteractions(listener);
    }
    
    @Test
    void findById_shouldReturnTransferProcess_whenValidCounterParty() {
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var token = claimToken();
        var agreement = contractAgreement();
    
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(token, agreement)).thenReturn(Result.success());
    
        var result = service.findById(processId, token);
    
        assertThat(result)
                .isSucceeded()
                .isEqualTo(transferProcess);
    }

    @Test
    void findById_shouldReturnNotFound_whenNegotiationNotFound() {
        when(store.findById(any())).thenReturn(null);
        
        var result = service.findById("invalidId", ClaimToken.Builder.newInstance().build());
        
        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }
    
    @Test
    void findById_shouldReturnNotFound_whenCounterPartyUnauthorized() {
        var processId = "transferProcessId";
        var transferProcess = transferProcess(INITIAL, processId);
        var token = claimToken();
        var agreement = contractAgreement();
        
        when(store.findById(processId)).thenReturn(transferProcess);
        when(negotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(validationService.validateRequest(token, agreement)).thenReturn(Result.failure("error"));
        
        var result = service.findById(processId, token);

        assertThat(result)
                .isFailed()
                .extracting(ServiceFailure::getReason)
                .isEqualTo(NOT_FOUND);
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTransferProcessNotFound(MethodCall<M> methodCall, M message) {
        when(store.findForCorrelationId(any())).thenReturn(null);

        var result = methodCall.call(service, message, claimToken());

        assertThat(result).matches(ServiceResult::failed);
        verify(store, never()).updateOrCreate(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    private TransferProcess transferProcess(TransferProcessStates state, String id) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id(id)
                .dataRequest(dataRequest())
                .build();
    }

    private ClaimToken claimToken() {
        return ClaimToken.Builder.newInstance()
                .claim("key", "value")
                .build();
    }

    private ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerId("provider")
                .consumerId("consumer")
                .assetId("asset")
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
        ServiceResult<?> call(TransferProcessProtocolService service, M message, ClaimToken token);
    }

    private static class NotFoundArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            MethodCall<TransferStartMessage> started = TransferProcessProtocolService::notifyStarted;
            MethodCall<TransferCompletionMessage> completed = TransferProcessProtocolService::notifyCompleted;
            MethodCall<TransferTerminationMessage> terminated = TransferProcessProtocolService::notifyTerminated;
            return Stream.of(
                    Arguments.of(started, TransferStartMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").build()),
                    Arguments.of(completed, TransferCompletionMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").build()),
                    Arguments.of(terminated, TransferTerminationMessage.Builder.newInstance().protocol("protocol")
                            .counterPartyAddress("http://any").processId("correlationId").code("TestCode")
                            .reason("TestReason").build())
            );
        }
    }
}
