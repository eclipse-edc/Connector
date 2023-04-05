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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
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

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
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
    private final TransferProcessManager manager = mock(TransferProcessManager.class);
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
                .id("correlationId")
                .protocol("protocol")
                .connectorAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isSucceeded().extracting(TransferProcess::getCorrelationId).isEqualTo("correlationId");
        verify(listener).preRequested(any());
        verify(store).save(argThat(t -> t.getState() == INITIAL.code()));
        verify(listener).requested(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyRequested_doNothingIfProcessAlreadyExist() {
        var message = TransferRequestMessage.Builder.newInstance()
                .id("correlationId")
                .protocol("protocol")
                .connectorAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        when(store.processIdForDataRequestId("correlationId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(transferProcess(REQUESTED, "processId"));

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isSucceeded().extracting(TransferProcess::getId).isEqualTo("processId");
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidAgreement_shouldNotInitiateTransfer() {
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();
        when(negotiationStore.findContractAgreement(any())).thenReturn(contractAgreement());
        when(validationService.validateAgreement(any(), any())).thenReturn(Result.failure("error"));
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyRequested_invalidDestination_shouldNotInitiateTransfer() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.failure("invalid data address"));
        var message = TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                .build();

        var result = service.notifyRequested(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyStarted_shouldTransitionToStarted() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(transferProcess(REQUESTED, "processId"));
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyStarted(message, claimToken());

        assertThat(result).isSucceeded();
        verify(listener).preStarted(any());
        verify(store).save(argThat(t -> t.getState() == STARTED.code()));
        verify(listener).started(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyStarted_shouldReturnConflict_whenStatusIsNotValid() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(COMPLETED.code()).build());
        var message = TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyStarted(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyCompleted_shouldTransitionToCompleted() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(transferProcess(STARTED, "processId"));
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyCompleted(message, claimToken());

        assertThat(result).isSucceeded();
        verify(listener).preCompleted(any());
        verify(store).save(argThat(t -> t.getState() == COMPLETED.code()));
        verify(listener).completed(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyCompleted_shouldReturnConflict_whenStatusIsNotValid() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(REQUESTED.code()).build());
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyCompleted(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void notifyTerminated_shouldTransitionToTerminated() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(transferProcess(STARTED, "processId"));
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyTerminated(message, claimToken());

        assertThat(result).isSucceeded();
        verify(listener).preTerminated(any());
        verify(store).save(argThat(t -> t.getState() == TERMINATED.code()));
        verify(listener).terminated(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void notifyTerminated_shouldReturnConflict_whenStatusIsNotValid() {
        when(store.processIdForDataRequestId("dataRequestId")).thenReturn("processId");
        when(store.find("processId")).thenReturn(TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(TERMINATED.code()).build());
        var message = TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://any")
                .processId("dataRequestId")
                .build();

        var result = service.notifyTerminated(message, claimToken());

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @ParameterizedTest
    @ArgumentsSource(NotFoundArguments.class)
    <M extends RemoteMessage> void notify_shouldFail_whenTransferProcessNotFound(MethodCall<M> methodCall, M message) {
        when(store.processIdForDataRequestId(any())).thenReturn(null);
        when(store.find(any())).thenReturn(null);

        var result = methodCall.call(service, message, claimToken());

        assertThat(result).matches(ServiceResult::failed);
        verifyNoInteractions(manager);
        verify(store, never()).save(any());
        verify(transactionContext, atLeastOnce()).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    private static class NotFoundArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            MethodCall<TransferStartMessage> started = TransferProcessProtocolService::notifyStarted;
            MethodCall<TransferCompletionMessage> completed = TransferProcessProtocolService::notifyCompleted;
            MethodCall<TransferTerminationMessage> terminated = TransferProcessProtocolService::notifyTerminated;
            return Stream.of(
                    Arguments.of(started, TransferStartMessage.Builder.newInstance().protocol("protocol")
                                    .connectorAddress("http://any").processId("dataRequestId").build()),
                    Arguments.of(completed, TransferCompletionMessage.Builder.newInstance().protocol("protocol")
                                    .connectorAddress("http://any").processId("dataRequestId").build()),
                    Arguments.of(terminated, TransferTerminationMessage.Builder.newInstance().protocol("protocol")
                                    .connectorAddress("http://any").processId("dataRequestId").build())
            );
        }
    }

    @FunctionalInterface
    private interface MethodCall<M extends RemoteMessage> {
        ServiceResult<?> call(TransferProcessProtocolService service, M message, ClaimToken token);
    }

    private TransferProcess transferProcess(TransferProcessStates state, String id) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id(id)
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
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId("asset")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
