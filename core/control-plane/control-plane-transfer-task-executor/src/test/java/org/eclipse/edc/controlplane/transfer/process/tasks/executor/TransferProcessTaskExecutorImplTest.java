/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.transfer.process.tasks.executor;

import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.processors.TransferProcessorsImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor;
import org.eclipse.edc.controlplane.transfer.spi.tasks.CompleteDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.PrepareTransfer;
import org.eclipse.edc.controlplane.transfer.spi.tasks.ResumeDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferRequest;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferStart;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SignalDataflowStarted;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SuspendDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TerminateDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessTaskExecutorImplTest {

    private final TransferProcessObservable observable = mock();
    private final DataFlowController dataFlowController = mock();
    private final TransferProcessStore transferStore = mock();
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final ProtocolWebhookResolver webhookResolver = mock();
    private final DataAddressResolver addressResolver = mock();
    private final PolicyArchive policyArchive = mock();
    private final TransferProcessPendingGuard pendingGuard = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final Monitor monitor = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final Clock clock = Clock.systemUTC();
    private final TaskService taskService = mock();
    private final String protocolWebhookUrl = "http://protocol.webhook/url";
    private final EntityRetryProcessConfiguration retryConfig = new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L));
    private final EntityRetryProcessFactory entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, retryConfig);

    private final TransferProcessors transferProcessors = new TransferProcessorsImpl(policyArchive, entityRetryProcessFactory, dataFlowController,
            dataAddressStore, observable, transferStore, monitor, addressResolver, webhookResolver, dispatcherRegistry);
    private TransferProcessTaskExecutor executor;

    @BeforeEach
    void setUp() {

        when(pendingGuard.test(any())).thenReturn(false);
        when(transferStore.save(any())).thenReturn(StoreResult.success());
        when(policyArchive.getAgreementIdForContract(any())).thenReturn("agreementId");

        executor = TransferProcessTaskExecutorImpl.Builder.newInstance()
                .store(transferStore)
                .pendingGuard(pendingGuard)
                .transactionContext(transactionContext)
                .monitor(monitor)
                .clock(clock)
                .taskService(taskService)
                .transferProcessors(transferProcessors)
                .build();
    }


    @ParameterizedTest
    @ArgumentsSource(StateTransitionProvider.class)
    void handle(TransferProcessTaskPayload payload, TransferProcessStates expectedState) {

        var contractId = "contractId";
        var dataAddress = DataAddress.Builder.newInstance().type("any").keyName("keyName").build();

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(dataFlowController.started(any())).thenReturn(StatusResult.success());
        when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
        when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
        when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
        when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
        when(addressResolver.resolveForAsset(any())).thenReturn(DataAddress.Builder.newInstance().type("type").build());
        when(dataAddressStore.resolve(any())).thenReturn(StoreResult.success(dataAddress));
        when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());
        when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(TransferProcessAck.Builder.newInstance().build())));
        when(webhookResolver.getWebhook(any(), any())).thenReturn(() -> protocolWebhookUrl);
        var transferProcess = TransferProcess.Builder.newInstance()
                .id(payload.getProcessId())
                .type(TransferProcess.Type.valueOf(payload.getProcessType()))
                .state(TransferProcessStates.from(payload.getProcessState()).code())
                .contractId(contractId)
                .build();

        when(transferStore.findById(payload.getProcessId())).thenReturn(transferProcess);
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().build());


        var result = executor.handle(payload);

        assertThat(result).isSucceeded();

        var captor = ArgumentCaptor.forClass(TransferProcess.class);
        verify(transferStore).save(captor.capture());

        var updatedProcess = captor.getValue();

        assertThat(updatedProcess.getState()).isEqualTo(expectedState.code());

    }

    @Test
    void handle_shouldSkipWhenTransferProcessNotFound() {
        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(transferStore.findById("transfer-123")).thenReturn(null);

        var result = executor.handle(task);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("not found");
    }

    @Test
    void handle_shouldSkipWhenTransferProcessIsInFinalState() {
        var transferProcess = createTransferProcess("transfer-123", COMPLETED);

        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(transferStore.findById("transfer-123")).thenReturn(transferProcess);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void handle_shouldSkipWhenStateDoesNotMatch() {
        var transferProcess = createTransferProcess("transfer-123", TransferProcessStates.PREPARATION_REQUESTED);

        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(transferStore.findById("transfer-123")).thenReturn(transferProcess);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void handle_shouldSkipWhenPendingGuardMatches() {
        var transferProcess = createTransferProcess("transfer-123", INITIAL);

        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(transferStore.findById("transfer-123")).thenReturn(transferProcess);
        when(pendingGuard.test(transferProcess)).thenReturn(true);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void handle_shouldFailWhenProcessTypeDoesNotMatch() {
        var transferProcess = createTransferProcess("transfer-123", INITIAL);

        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(transferStore.findById("transfer-123")).thenReturn(transferProcess);

        var result = executor.handle(task);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void handle_shouldTransitionWhenHandlerSucceeds() {
        var transferProcess = createTransferProcess("transfer-123", INITIAL);
        var dataPlaneId = UUID.randomUUID().toString();
        var dataFlowResponse = DataFlowResponse.Builder.newInstance()
                .dataPlaneId(dataPlaneId)
                .async(true)
                .build();

        var task = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(INITIAL.code())
                .processType(TransferProcess.Type.CONSUMER.name())
                .build();

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferStore.findById("transfer-123")).thenReturn(transferProcess);
        when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
        verify(transferStore).save(any());
    }

    private TransferProcess createTransferProcess(String id, TransferProcessStates state) {
        var transferProcess = mock(TransferProcess.class);
        when(transferProcess.getId()).thenReturn(id);
        when(transferProcess.getState()).thenReturn(state.code());
        when(transferProcess.stateAsString()).thenReturn(state.name());
        when(transferProcess.getType()).thenReturn(TransferProcess.Type.CONSUMER);
        when(transferProcess.getParticipantContextId()).thenReturn("participant-123");
        when(transferProcess.getProtocol()).thenReturn("DSP");
        when(transferProcess.getCounterPartyAddress()).thenReturn("http://counter-party");
        when(transferProcess.getContractId()).thenReturn("contract-123");
        when(transferProcess.getAssetId()).thenReturn("asset-123");

        return transferProcess;
    }

    public static class StateTransitionProvider implements ArgumentsProvider {

        protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, String id, TransferProcessStates state, TransferProcess.Type type) {
            return builder.processId(id)
                    .processState(state.code())
                    .processType(type.name());
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var id = UUID.randomUUID().toString();
            return Stream.of(
                    arguments(baseBuilder(PrepareTransfer.Builder.newInstance(), id, INITIAL, CONSUMER).build(), REQUESTING),
                    arguments(baseBuilder(SendTransferRequest.Builder.newInstance(), id, REQUESTING, CONSUMER).build(), REQUESTED),
                    arguments(baseBuilder(SignalDataflowStarted.Builder.newInstance(), id, REQUESTED, CONSUMER).build(), STARTED),
                    arguments(baseBuilder(SuspendDataFlow.Builder.newInstance(), id, SUSPENDING, CONSUMER).build(), SUSPENDED),
                    arguments(baseBuilder(ResumeDataFlow.Builder.newInstance(), id, RESUMING, CONSUMER).build(), RESUMED),
                    arguments(baseBuilder(TerminateDataFlow.Builder.newInstance(), id, TERMINATING, CONSUMER).build(), TERMINATED),
                    arguments(baseBuilder(CompleteDataFlow.Builder.newInstance(), id, COMPLETING, CONSUMER).build(), COMPLETED),

                    arguments(baseBuilder(PrepareTransfer.Builder.newInstance(), id, INITIAL, PROVIDER).build(), STARTING),
                    arguments(baseBuilder(SendTransferStart.Builder.newInstance(), id, STARTING, PROVIDER).build(), STARTED),
                    arguments(baseBuilder(SuspendDataFlow.Builder.newInstance(), id, SUSPENDING, PROVIDER).build(), SUSPENDED),
                    arguments(baseBuilder(ResumeDataFlow.Builder.newInstance(), id, RESUMING, PROVIDER).build(), STARTED),
                    arguments(baseBuilder(TerminateDataFlow.Builder.newInstance(), id, TERMINATING, PROVIDER).build(), TERMINATED),
                    arguments(baseBuilder(CompleteDataFlow.Builder.newInstance(), id, COMPLETING, PROVIDER).build(), COMPLETED)
            );

        }
    }
}
