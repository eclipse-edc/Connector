/*
 *  Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.processors;

import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PREPARATION_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessorsImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final int RETRY_LIMIT = 1;

    private final TransferProcessStore store = mock();
    private final PolicyArchive policyArchive = mock();
    private final DataFlowController dataFlowController = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final TransferProcessListener listener = mock();
    private final ProtocolWebhookResolver protocolWebhookResolver = mock();
    private final DataAddressResolver addressResolver = mock();
    private final ProtocolRemoteMessageDispatcher messageDispatcher = mock();

    private TransferProcessorsImpl processors;

    @BeforeEach
    void setUp() {
        when(store.save(any())).thenReturn(StoreResult.success());
        when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().assignee("consumerId").assigner("providerId").build());
        when(policyArchive.getAgreementIdForContract(any())).thenReturn("agreementId");
        when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://protocol.webhook/url");
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L));
        var entityRetryProcessFactory = new EntityRetryProcessFactory(mock(), Clock.systemUTC(), entityRetryProcessConfiguration);
        processors = new TransferProcessorsImpl(policyArchive, entityRetryProcessFactory, dataFlowController,
                dataAddressStore, observable, store, mock(), addressResolver, protocolWebhookResolver, messageDispatcher);
    }

    @Nested
    class ProcessConsumerInitial {

        @Test
        void shouldTransitionToTerminated_whenNoPolicyFound() {
            var process = processBuilder(INITIAL).type(CONSUMER).build();
            when(policyArchive.findPolicyForContract(any())).thenReturn(null);

            processors.processConsumerInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToPreparationRequested_whenAsyncProvision() {
            var dataPlaneId = UUID.randomUUID().toString();
            var process = processBuilder(INITIAL).type(CONSUMER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataPlaneId(dataPlaneId).async(true).build();
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            processors.processConsumerInitial(process).join();

            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(store).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(PREPARATION_REQUESTED.code());
            assertThat(captor.getValue().getDataPlaneId()).isEqualTo(dataPlaneId);
            verify(listener).preparationRequested(process);
        }

        @Test
        void shouldTransitionToRequesting_whenSyncProvisionWithDataAddress() {
            var dataAddress = DataAddress.Builder.newInstance().type("any").build();
            var process = processBuilder(INITIAL).type(CONSUMER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataAddress(dataAddress).async(false).build();
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            processors.processConsumerInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == REQUESTING.code()));
            verify(dataAddressStore).store(dataAddress, process);
        }

        @Test
        void shouldTransitionToRequesting_whenSyncProvisionWithoutDataAddress() {
            var process = processBuilder(INITIAL).type(CONSUMER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().async(false).build();
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            processors.processConsumerInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == REQUESTING.code()));
            verify(dataAddressStore, never()).store(any(), any());
        }

        @Test
        void shouldTransitionToTerminated_whenProvisionFails() {
            var process = processBuilder(INITIAL).type(CONSUMER).build();
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.fatalError("error"));

            processors.processConsumerInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }
    }

    @Nested
    class ProcessProviderInitial {

        @Test
        void shouldTransitionToTerminated_whenNoPolicyFound() {
            var process = processBuilder(INITIAL).type(PROVIDER).build();
            when(policyArchive.findPolicyForContract(any())).thenReturn(null);

            processors.processProviderInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToStartupRequested_whenAsyncDataFlowStart() {
            var dataPlaneId = UUID.randomUUID().toString();
            var process = processBuilder(INITIAL).type(PROVIDER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataPlaneId(dataPlaneId).async(true).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            processors.processProviderInitial(process).join();

            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(store).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(STARTUP_REQUESTED.code());
            assertThat(captor.getValue().getDataPlaneId()).isEqualTo(dataPlaneId);
            verifyNoInteractions(dataAddressStore);
        }

        @Test
        void shouldTransitionToStarting_whenSyncDataFlowWithoutDataAddress() {
            var dataPlaneId = UUID.randomUUID().toString();
            var process = processBuilder(INITIAL).type(PROVIDER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataPlaneId(dataPlaneId).async(false).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            processors.processProviderInitial(process).join();

            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(store).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(STARTING.code());
            assertThat(captor.getValue().getDataPlaneId()).isEqualTo(dataPlaneId);
            verifyNoInteractions(dataAddressStore);
        }

        @Test
        void shouldTransitionToStarting_whenSyncDataFlowWithDataAddress() {
            var dataPlaneId = UUID.randomUUID().toString();
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            var process = processBuilder(INITIAL).type(PROVIDER).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataPlaneId(dataPlaneId).dataAddress(dataAddress).async(false).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            processors.processProviderInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == STARTING.code()));
            verify(dataAddressStore).store(dataAddress, process);
        }

        @Test
        void shouldTransitionToTerminating_whenDataFlowStartFails() {
            var process = processBuilder(INITIAL).type(PROVIDER).stateCount(RETRY_LIMIT + 1).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.fatalError("error"));

            processors.processProviderInitial(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessRequesting {

        @Test
        void shouldSendRequestAndTransitionToRequested() {
            var process = processBuilder(REQUESTING).type(CONSUMER).correlationId(null).build();
            var ack = TransferProcessAck.Builder.newInstance().providerPid("providerPid").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processRequesting(process).join();

            var captor = ArgumentCaptor.<TransferRequestMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(TransferProcessAck.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getCallbackAddress()).isEqualTo("http://protocol.webhook/url");
            assertThat(message.getProcessId()).isEqualTo(process.getId());
            assertThat(message.getContractId()).isEqualTo("agreementId");
            assertThat(message.getDataAddress()).isNull();
            verify(store).save(argThat(p -> p.getState() == REQUESTED.code() && "providerPid".equals(p.getCorrelationId())));
            verify(listener).requested(process);
        }

        @Test
        void shouldIncludeDataAddress_whenPresent() {
            var dataAddress = DataAddress.Builder.newInstance().type("any").build();
            var process = processBuilder(REQUESTING).type(CONSUMER).build();
            var ack = TransferProcessAck.Builder.newInstance().providerPid("providerPid").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.success(dataAddress));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processRequesting(process).join();

            var captor = ArgumentCaptor.<TransferRequestMessage>captor();
            verify(messageDispatcher).dispatch(any(), any(), captor.capture());
            assertThat(captor.getValue().getDataAddress()).isSameAs(dataAddress);
        }

        @Test
        void shouldTransitionToTerminated_whenNoWebhookFound() {
            var process = processBuilder(REQUESTING).type(CONSUMER).build();
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(null);

            processors.processRequesting(process).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToTerminated_whenNoAgreementFound() {
            var process = processBuilder(REQUESTING).type(CONSUMER).build();
            when(policyArchive.getAgreementIdForContract(any())).thenReturn(null);

            processors.processRequesting(process).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToTerminated_whenRetriesExhausted() {
            var process = processBuilder(REQUESTING).type(CONSUMER).stateCount(RETRY_LIMIT + 1).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("any"));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processRequesting(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }
    }

    @Nested
    class ProcessStartupRequested {

        @Test
        void shouldNotifyDataPlaneAndTransitionToStarted() {
            var process = processBuilder(STARTUP_REQUESTED).type(CONSUMER).build();
            when(dataFlowController.started(any())).thenReturn(StatusResult.success());
            when(store.findById(process.getId())).thenReturn(process);

            processors.processStartupRequested(process).join();

            verify(dataFlowController).started(process);
            verify(store).save(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        }

        @Test
        void shouldTransitionToTerminating_whenNotifyFails() {
            var process = processBuilder(STARTUP_REQUESTED).type(CONSUMER).stateCount(RETRY_LIMIT + 1).build();
            when(dataFlowController.started(any())).thenReturn(StatusResult.fatalError("error"));

            processors.processStartupRequested(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessStarting {

        @Test
        void shouldSendStartMessageAndTransitionToStarted_whenNoDataAddress() {
            var process = processBuilder(STARTING).type(PROVIDER).correlationId("correlationId").build();
            when(messageDispatcher.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processStarting(process).join();

            var captor = ArgumentCaptor.<TransferStartMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo(process.getId());
            assertThat(message.getConsumerPid()).isEqualTo("correlationId");
            assertThat(message.getDataAddress()).isNull();
            verify(store).save(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        }

        @Test
        void shouldIncludeDataAddress_whenPresent() {
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            var process = processBuilder(STARTING).type(PROVIDER).build();
            when(messageDispatcher.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.success(dataAddress));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processStarting(process).join();

            var captor = ArgumentCaptor.<TransferStartMessage>captor();
            verify(messageDispatcher).dispatch(any(), any(), captor.capture());
            assertThat(captor.getValue().getDataAddress()).isSameAs(dataAddress);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var process = processBuilder(STARTING).type(PROVIDER).stateCount(RETRY_LIMIT + 1).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processStarting(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessResuming {

        @Test
        void provider_shouldResumeDataFlowAndSendStartMessage() {
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            var process = processBuilder(RESUMING).type(PROVIDER).dataAddressOwner(true).build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance().dataAddress(dataAddress).build();
            when(dataFlowController.resume(any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(messageDispatcher.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());
            when(store.findById(process.getId())).thenReturn(process);

            processors.processResuming(process).join();

            verify(dataFlowController).resume(process);
            var captor = ArgumentCaptor.<TransferStartMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            assertThat(captor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(dataAddress);
            verify(store).save(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        }

        @Test
        void consumer_shouldSkipDispatchAndTransitionToStarted() {
            var process = processBuilder(RESUMING).type(CONSUMER).dataAddressOwner(false).build();
            when(dataFlowController.resume(any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processResuming(process).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        }

        @Test
        void consumer_shouldSendStartMessageAndTransitionToResumed_whenResumingRequested() {
            var process = processBuilder(RESUMING_REQUESTED).type(CONSUMER).correlationId("correlationId").build();
            when(messageDispatcher.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processResuming(process).join();

            var captor = ArgumentCaptor.<TransferStartMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getConsumerPid()).isEqualTo(process.getId());
            assertThat(message.getProviderPid()).isEqualTo("correlationId");
            assertThat(message.getDataAddress()).isNull();
            verify(store).save(argThat(p -> p.getState() == RESUMED.code()));
            verify(listener).resumed(process);
        }
    }

    @Nested
    class ProcessCompleting {

        @Test
        void provider_shouldSendCompletionMessageAndTransitionToCompleted() {
            var process = processBuilder(COMPLETING).type(PROVIDER).correlationId("correlationId").build();
            when(messageDispatcher.dispatch(any(), any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processCompleting(process).join();

            var captor = ArgumentCaptor.<TransferCompletionMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo(process.getId());
            assertThat(message.getConsumerPid()).isEqualTo("correlationId");
            verify(store).save(argThat(p -> p.getState() == COMPLETED.code()));
            verify(listener).completed(process);
            verify(dataAddressStore).remove(process);
        }

        @Test
        void consumer_shouldSendCompletionMessageAndTransitionToCompleted() {
            var process = processBuilder(COMPLETING).type(CONSUMER).correlationId("correlationId").build();
            when(messageDispatcher.dispatch(any(), any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processCompleting(process).join();

            var captor = ArgumentCaptor.<TransferCompletionMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo("correlationId");
            assertThat(message.getConsumerPid()).isEqualTo(process.getId());
            verify(store).save(argThat(p -> p.getState() == COMPLETED.code()));
            verify(listener).completed(process);
        }

        @Test
        void shouldNotifyDataFlowCompletion_whenCompletedByCounterParty() {
            var process = processBuilder(COMPLETING).type(CONSUMER).build();
            process.transitionCompletingRequested();
            when(store.save(any())).thenReturn(StoreResult.success());
            when(dataFlowController.completed(any())).thenReturn(StatusResult.success());
            when(store.findById(process.getId())).thenReturn(process);

            processors.processCompleting(process).join();

            verify(dataFlowController).completed(process);
            verifyNoInteractions(messageDispatcher);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var process = processBuilder(COMPLETING).type(CONSUMER).stateCount(RETRY_LIMIT + 1).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processCompleting(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessSuspending {

        @Test
        void provider_shouldSuspendDataFlowAndSendSuspensionMessage() {
            var process = processBuilder(SUSPENDING).type(PROVIDER).correlationId("correlationId").build();
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processSuspending(process).join();

            verify(dataFlowController).suspend(process);
            var captor = ArgumentCaptor.<TransferSuspensionMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo(process.getId());
            assertThat(message.getConsumerPid()).isEqualTo("correlationId");
            verify(store).save(argThat(p -> p.getState() == SUSPENDED.code()));
            verify(listener).suspended(process);
        }

        @Test
        void consumer_shouldSuspendDataFlowAndSendSuspensionMessage() {
            var process = processBuilder(SUSPENDING).type(CONSUMER).correlationId("correlationId").build();
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processSuspending(process).join();

            var captor = ArgumentCaptor.<TransferSuspensionMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo("correlationId");
            assertThat(message.getConsumerPid()).isEqualTo(process.getId());
            verify(store).save(argThat(p -> p.getState() == SUSPENDED.code()));
        }

        @Test
        void shouldNotSendMessage_whenSuspensionWasRequestedByCounterParty() {
            var process = processBuilder(SUSPENDING_REQUESTED).type(PROVIDER).correlationId("correlationId").build();
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
            when(store.findById(process.getId())).thenReturn(process);

            processors.processSuspending(process).join();

            verify(dataFlowController).suspend(process);
            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(p -> p.getState() == SUSPENDED.code()));
            verify(listener).suspended(process);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var process = processBuilder(SUSPENDING).type(PROVIDER).stateCount(RETRY_LIMIT + 1).build();
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processSuspending(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessTerminating {

        @Test
        void consumer_shouldTransitionDirectlyToTerminated_whenBeforeRequested() {
            var process = processBuilder(INITIAL).type(CONSUMER).build();

            processors.processTerminating(process).join();

            verifyNoInteractions(messageDispatcher, dataFlowController);
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void provider_shouldTerminateDataFlowAndSendTerminationMessage() {
            var process = processBuilder(TERMINATING).type(PROVIDER).correlationId("correlationId").build();
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(messageDispatcher.dispatch(any(), any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processTerminating(process).join();

            verify(dataFlowController).terminate(process);
            var captor = ArgumentCaptor.<TransferTerminationMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo(process.getId());
            assertThat(message.getConsumerPid()).isEqualTo("correlationId");
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
            verify(dataAddressStore).remove(process);
        }

        @Test
        void consumer_shouldTerminateDataFlowAndSendTerminationMessage() {
            var process = processBuilder(TERMINATING).type(CONSUMER).correlationId("correlationId").build();
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(messageDispatcher.dispatch(any(), any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(process.getId())).thenReturn(process);

            processors.processTerminating(process).join();

            verify(dataFlowController).terminate(process);
            var captor = ArgumentCaptor.<TransferTerminationMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProviderPid()).isEqualTo("correlationId");
            assertThat(message.getConsumerPid()).isEqualTo(process.getId());
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
        }

        @Test
        void shouldNotSendMessage_whenTerminationWasRequestedByCounterParty() {
            var process = processBuilder(TERMINATING).type(PROVIDER).correlationId("correlationId").build();
            process.transitionTerminatingRequested("reason");
            when(store.save(any())).thenReturn(StoreResult.success());
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(store.findById(process.getId())).thenReturn(process);

            processors.processTerminating(process).join();

            verify(dataFlowController).terminate(process);
            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToTerminated_whenRetriesExhausted() {
            var process = processBuilder(TERMINATING).type(PROVIDER).stateCount(RETRY_LIMIT + 1).build();
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.fatalError("error"));

            processors.processTerminating(process).join();

            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
        }
    }

    private TransferProcess.Builder processBuilder(org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .id("test-process-" + UUID.randomUUID())
                .type(CONSUMER)
                .state(state.code())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyAddress("http://counter.party/address")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .protocol("protocol");
    }
}
