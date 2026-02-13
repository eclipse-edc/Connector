/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.process;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PREPARATION_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class TransferProcessManagerImplTest {

    public static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final String DESTINATION_TYPE = "test-type";
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private static final int RETRY_LIMIT = 1;
    private static final int RETRY_EXHAUSTED = RETRY_LIMIT + 1;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final TransferProcessStore transferProcessStore = mock();
    private final PolicyArchive policyArchive = mock();
    private final DataFlowController dataFlowController = mock();
    private final Clock clock = Clock.systemUTC();
    private final TransferProcessListener listener = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private final DataAddressResolver addressResolver = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final String protocolWebhookUrl = "http://protocol.webhook/url";
    private final TransferProcessPendingGuard pendingGuard = mock();

    private TransferProcessManagerImpl manager;

    private static DataFlowResponse.Builder dataFlowResponseBuilder() {
        return DataFlowResponse.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance()
                        .type("type")
                        .build());
    }

    @BeforeEach
    void setup() {
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> protocolWebhookUrl);
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().build());
        when(policyArchive.getAgreementIdForContract(any())).thenReturn("agreementId");
        when(transferProcessStore.save(any())).thenReturn(StoreResult.success());
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L));
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .dataFlowController(dataFlowController)
                .waitStrategy(() -> 10000L)
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock())
                .clock(clock)
                .observable(observable)
                .store(transferProcessStore)
                .policyArchive(policyArchive)
                .addressResolver(addressResolver)
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .pendingGuard(pendingGuard)
                .dataAddressStore(dataAddressStore)
                .build();
    }

    @Test
    void pendingGuard_shouldSetTheTransferPending_whenPendingGuardMatches() {
        when(pendingGuard.test(any())).thenReturn(true);
        var process = createTransferProcessBuilder(STARTING).build();
        when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(STARTING.code());
            assertThat(saved.isPending()).isTrue();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DispatchFailureArguments.class)
    void dispatchFailure(TransferProcessStates starting, TransferProcessStates ending, CompletableFuture<StatusResult<Object>> result, UnaryOperator<TransferProcess.Builder> builderEnricher) {
        var transferProcess = builderEnricher.apply(createTransferProcessBuilder(starting).state(starting.code())).build();
        when(transferProcessStore.nextNotLeased(anyInt(), or(stateIs(starting.code()), or(consumerStateIs(starting.code()), providerStateIs(starting.code())))))
                .thenReturn(List.of(transferProcess)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(result);
        when(transferProcessStore.findById(transferProcess.getId())).thenReturn(transferProcess);
        when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponseBuilder().build()));
        when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
        when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
        when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());
        when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("any"));


        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore).save(captor.capture());
            assertThat(captor.getAllValues()).hasSize(1).first().satisfies(n -> {
                assertThat(n.getState()).isEqualTo(ending.code());
            });
            verify(dispatcherRegistry, only()).dispatch(any(), any(), any());
        });
    }

    @Nested
    class Completing {

        @Test
        void provider_shouldTransitionToCompleted_whenSendingMessageSucceed() {
            var process = createTransferProcessBuilder(COMPLETING).type(PROVIDER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(COMPLETING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferCompletionMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getConsumerPid()).isEqualTo("correlationId");
                assertThat(message.getProcessId()).isEqualTo("correlationId");
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == COMPLETED.code()));
                verify(listener).completed(process);
                verify(dataAddressStore).remove(process);
            });
        }

        @Test
        void consumer_shouldTransitionToComplete_whenSendingMessageSucceed() {
            var process = createTransferProcessBuilder(COMPLETING).type(CONSUMER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(COMPLETING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferCompletionMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo("correlationId");
                assertThat(message.getConsumerPid()).isEqualTo(process.getId());
                assertThat(message.getProcessId()).isEqualTo("correlationId");
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == COMPLETED.code()));
                verify(listener).completed(process);
            });
        }

        @Test
        void shouldNotifyDataFlowCompletion_whenTransferCompletedByCounterPart() {
            var process = createTransferProcessBuilder(COMPLETING_REQUESTED).type(CONSUMER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(COMPLETING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING_REQUESTED.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).completed(process);
            });
        }
    }

    @Nested
    class TerminatingProvider {
        @Test
        void shouldTransitionToTerminated_whenMessageSentCorrectly() {
            var process = createTransferProcessBuilder(TERMINATING).type(PROVIDER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).terminate(process);
                var captor = ArgumentCaptor.forClass(TransferTerminationMessage.class);
                verify(dispatcherRegistry).dispatch(any(), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getConsumerPid()).isEqualTo("correlationId");
                assertThat(message.getProcessId()).isEqualTo("correlationId");
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == TERMINATED.code()));
                verify(listener).terminated(process);
                verify(dataAddressStore).remove(process);
            });
        }

        @Test
        void shouldUpdateStateCountForRetry_whenFailureAndRetriesNotExhausted() {
            var process = createTransferProcess(TERMINATING).toBuilder().type(PROVIDER).build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == TERMINATING.code()));
            });
        }

        @Test
        void shouldTransitionToTerminatedIfFatalFailure() {
            var process = createTransferProcess(TERMINATING).toBuilder().type(PROVIDER).build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.failure(FATAL_ERROR));

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATED.code()));
            });
        }

        @Test
        void shouldTransitToTerminated_whenFailureAndRetriesExhausted() {
            var process = createTransferProcessBuilder(TERMINATING).type(PROVIDER).stateCount(RETRY_EXHAUSTED).build();
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.failure(ERROR_RETRY));
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

            manager.start();

            await().untilAsserted(() -> {
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == TERMINATED.code()));
            });
        }
    }

    @Nested
    class TerminatingConsumer {
        @Test
        void shouldTransitionToTerminated_whenMessageSentCorrectly() {
            var process = createTransferProcessBuilder(TERMINATING).type(CONSUMER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).terminate(process);
                var captor = ArgumentCaptor.forClass(TransferTerminationMessage.class);
                verify(dispatcherRegistry).dispatch(any(), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo("correlationId");
                assertThat(message.getConsumerPid()).isEqualTo(process.getId());
                assertThat(message.getProcessId()).isEqualTo("correlationId");
                verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATED.code()));
                verify(listener).terminated(process);
                verify(dataAddressStore).remove(process);
            });
        }

    }

    @Nested
    class TerminatingRequestedProvider {
        @Test
        void shouldNotSendTheMessage_whenConsumerRequestedTermination() {
            var process = createTransferProcessBuilder(TERMINATING_REQUESTED).type(PROVIDER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING_REQUESTED.code()).build());
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).terminate(process);
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == TERMINATED.code()));
                verify(listener).terminated(process);
            });
        }

        @Test
        void shouldTransitionToTerminatingRequested_whenDataFlowTerminationFails() {
            var process = createTransferProcessBuilder(TERMINATING_REQUESTED).type(PROVIDER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING_REQUESTED.code()).build());
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).terminate(process);
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == TERMINATING_REQUESTED.code()));
            });
        }

    }

    @Nested
    class TerminatingRequestedConsumer {
        @Test
        void shouldNotSendTheMessage_whenProviderRequestedTermination() {
            var process = createTransferProcessBuilder(TERMINATING_REQUESTED).type(CONSUMER).correlationId("correlationId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(TERMINATING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING_REQUESTED.code()).build());
            when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATED.code()));
                verify(listener).terminated(process);
            });
        }

    }

    @Nested
    class InitiateConsumerRequest {
        @Test
        void shouldStoreTransferProcess() {
            when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().target("assetId").build());
            when(transferProcessStore.findForCorrelationId("1")).thenReturn(null);
            var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
            var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().label("label").build();
            var transferRequest = TransferRequest.Builder.newInstance()
                    .id("1")
                    .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                    .callbackAddresses(List.of(callback))
                    .dataplaneMetadata(dataplaneMetadata)
                    .build();
            var participantContext = ParticipantContext.Builder.newInstance().participantContextId("id")
                    .identity("identity")
                    .build();

            var result = manager.initiateConsumerRequest(participantContext, transferRequest);

            assertThat(result).isSucceeded().isNotNull();
            var captor = ArgumentCaptor.forClass(TransferProcess.class);
            verify(transferProcessStore, times(RETRY_LIMIT)).save(captor.capture());
            var transferProcess = captor.getValue();
            assertThat(transferProcess.getId()).isEqualTo("1");
            assertThat(transferProcess.getCorrelationId()).isNull();
            assertThat(transferProcess.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
            assertThat(transferProcess.getAssetId()).isEqualTo("assetId");
            assertThat(transferProcess.getDataplaneMetadata()).isSameAs(dataplaneMetadata);
            verify(listener).initiated(any());
        }

        @Test
        void shouldFail_whenPolicyNotAvailable() {
            when(policyArchive.findPolicyForContract(any())).thenReturn(null);
            when(transferProcessStore.findForCorrelationId("1")).thenReturn(null);

            var transferRequest = TransferRequest.Builder.newInstance()
                    .id("1")
                    .contractId("contractId")
                    .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                    .build();

            var participantContext = ParticipantContext.Builder.newInstance()
                    .participantContextId("participantContextId").identity("id").build();
            var result = manager.initiateConsumerRequest(participantContext, transferRequest);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class InitialConsumer {

        @Test
        void shouldTransitionToTerminated_whenNoPolicyFound() {
            var transferProcess = createTransferProcess(INITIAL);
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.failure(FATAL_ERROR));
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(INITIAL.code())))
                    .thenReturn(List.of(transferProcess))
                    .thenReturn(emptyList());
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(null);

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATED.code()));
            });
        }

        @Test
        void shouldTransitionToPreparationRequested_whenProvisionThroughDataplaneSucceeds() {
            var dataPlaneId = UUID.randomUUID().toString();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance()
                    .dataPlaneId(dataPlaneId)
                    .async(true)
                    .build();
            var transferProcess = createTransferProcess(INITIAL);
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(INITIAL.code())))
                    .thenReturn(List.of(transferProcess))
                    .thenReturn(emptyList());
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(listener).preparationRequested(transferProcess);
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var storedTransferProcess = captor.getValue();
                assertThat(storedTransferProcess.getState()).isEqualTo(PREPARATION_REQUESTED.code());
                assertThat(storedTransferProcess.getDataPlaneId()).isEqualTo(dataPlaneId);
            });
        }

        @Test
        void shouldTransitionToRequesting_whenProvisionThroughDataplaneSucceedsButNoActualProvisionNeeded() {
            var dataPlaneId = UUID.randomUUID().toString();
            var dataAddress = DataAddress.Builder.newInstance().type("any").build();
            var dataFlowResponse = DataFlowResponse.Builder.newInstance()
                    .dataPlaneId(dataPlaneId)
                    .dataAddress(dataAddress)
                    .async(false)
                    .build();
            var transferProcess = createTransferProcess(INITIAL);
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(INITIAL.code())))
                    .thenReturn(List.of(transferProcess))
                    .thenReturn(emptyList());
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var storedTransferProcess = captor.getValue();
                assertThat(storedTransferProcess.getState()).isEqualTo(REQUESTING.code());
                assertThat(storedTransferProcess.getDataPlaneId()).isEqualTo(dataPlaneId);
                verify(dataAddressStore).store(dataAddress, transferProcess);
            });
        }

        @Test
        void shouldTransitionToTerminated_whenProvisionThroughDataplaneFails() {
            var transferProcess = createTransferProcess(INITIAL);
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(INITIAL.code())))
                    .thenReturn(List.of(transferProcess))
                    .thenReturn(emptyList());
            when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.fatalError("error"));

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var storedTransferProcess = captor.getValue();
                assertThat(storedTransferProcess.getState()).isEqualTo(TERMINATED.code());
                assertThat(storedTransferProcess.getDataPlaneId()).isEqualTo(null);
            });
        }
    }

    @Nested
    class InitialProvider {

        private final TransferProcess.Builder builder = createTransferProcessBuilder(INITIAL).type(PROVIDER);

        @Test
        void shouldTransitionToStartupRequested_whenAsyncDataFlowStart() {
            var dataPlaneId = UUID.randomUUID().toString();
            var transferProcess = builder.build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
            when(addressResolver.resolveForAsset(any())).thenReturn(null);
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(
                    dataFlowResponseBuilder().async(true).dataPlaneId(dataPlaneId).build()));

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dataFlowController).start(transferProcess, Policy.Builder.newInstance().build());
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var actualTransferProcess = captor.getValue();
                assertThat(actualTransferProcess.getState()).isEqualTo(STARTUP_REQUESTED.code());
                assertThat(actualTransferProcess.getDataPlaneId()).isEqualTo(dataPlaneId);
                verifyNoInteractions(dataAddressStore);
            });
        }

        @Test
        void shouldTransitionToStarting_whenSyncDataFlowWithoutDataAddress() {
            var dataPlaneId = UUID.randomUUID().toString();
            var transferProcess = builder.build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
            var contentDataAddress = DataAddress.Builder.newInstance().type("type").build();
            when(addressResolver.resolveForAsset(any())).thenReturn(contentDataAddress);
            var dataFlowResponse = dataFlowResponseBuilder().async(false).dataAddress(null).dataPlaneId(dataPlaneId).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var actualTransferProcess = captor.getValue();
                assertThat(actualTransferProcess.getState()).isEqualTo(STARTING.code());
                assertThat(actualTransferProcess.getDataPlaneId()).isEqualTo(dataPlaneId);
                assertThat(actualTransferProcess.getContentDataAddress()).isSameAs(contentDataAddress);
                verifyNoInteractions(dataAddressStore);
            });
        }

        @Test
        void shouldTransitionToStarting_whenSyncDataFlowStartWithDataAddress() {
            var dataPlaneId = UUID.randomUUID().toString();
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            var transferProcess = builder.build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(INITIAL.code()))).thenReturn(List.of(transferProcess)).thenReturn(emptyList());
            when(addressResolver.resolveForAsset(any())).thenReturn(null);
            var dataFlowResponse = dataFlowResponseBuilder().async(false).dataAddress(dataAddress).dataPlaneId(dataPlaneId).build();
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dataFlowController).start(transferProcess, Policy.Builder.newInstance().build());
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(captor.capture());
                var actualTransferProcess = captor.getValue();
                assertThat(actualTransferProcess.getState()).isEqualTo(STARTING.code());
                assertThat(actualTransferProcess.getDataPlaneId()).isEqualTo(dataPlaneId);
                verify(dataAddressStore).store(dataAddress, transferProcess);
            });
        }

    }

    @Nested
    class Requesting {

        @Test
        void shouldSendMessageAndTransitionToRequested() {
            var process = createTransferProcessBuilder(REQUESTING).build();
            process.setCorrelationId(null);
            var ack = TransferProcessAck.Builder.newInstance().providerPid("providerPid").build();
            when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(REQUESTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));

            manager.start();

            await().untilAsserted(() -> {
                var storeCaptor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore, times(1)).save(storeCaptor.capture());
                var storedTransferProcess = storeCaptor.getValue();
                assertThat(storedTransferProcess.getState()).isEqualTo(REQUESTED.code());
                assertThat(storedTransferProcess.getCorrelationId()).isEqualTo("providerPid");
                verify(listener).requested(process);
                var captor = ArgumentCaptor.forClass(TransferRequestMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(TransferProcessAck.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProcessId()).isEqualTo(process.getId());
                assertThat(message.getConsumerPid()).isEqualTo(process.getId());
                assertThat(message.getProviderPid()).isEqualTo(null);
                assertThat(message.getCallbackAddress()).isEqualTo(protocolWebhookUrl);
                assertThat(message.getDataAddress()).isNull();
            });
        }

        @Test
        void shouldAddDataDataAddressToMessage_whenItExists() {
            var dataAddress = DataAddress.Builder.newInstance().type("any").keyName("keyName").build();
            var process = createTransferProcessBuilder(REQUESTING).build();
            var ack = TransferProcessAck.Builder.newInstance().providerPid("providerPid").build();
            when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(REQUESTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.success(dataAddress));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferRequestMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(TransferProcessAck.class), captor.capture());
                verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == REQUESTED.code()));
                verify(listener).requested(process);
                var requestMessage = captor.getValue();
                assertThat(requestMessage.getDataAddress()).isSameAs(dataAddress);
            });
        }
    }

    @Nested
    class Starting {

        @Test
        void shouldSendMessageToConsumer_whenNoDataAddress() {
            var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process);
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("not found"));

            manager.start();

            await().untilAsserted(() -> {
                var messageCaptor = ArgumentCaptor.forClass(TransferStartMessage.class);
                var storedCaptor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), messageCaptor.capture());
                verify(transferProcessStore).save(storedCaptor.capture());
                verify(listener).started(eq(process), any());
                var message = messageCaptor.getValue();
                assertThat(message.getProcessId()).isEqualTo(process.getCorrelationId());
                assertThat(message.getConsumerPid()).isEqualTo(process.getCorrelationId());
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getDataAddress()).isNull();
                var transferProcess = storedCaptor.getValue();
                assertThat(transferProcess.getState()).isEqualTo(STARTED.code());
                verify(dataAddressStore, never()).store(any(), any());
            });
        }

        @Test
        void shouldSendMessageToConsumer_whenDataAddress() {
            var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(STARTING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process);
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            var dataAddress = DataAddress.Builder.newInstance().type("type").build();
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.success(dataAddress));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var messageCaptor = ArgumentCaptor.forClass(TransferStartMessage.class);
                var storedCaptor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), messageCaptor.capture());
                verify(transferProcessStore).save(storedCaptor.capture());
                verify(listener).started(eq(process), any());
                var message = messageCaptor.getValue();
                assertThat(message.getProcessId()).isEqualTo(process.getCorrelationId());
                assertThat(message.getConsumerPid()).isEqualTo(process.getCorrelationId());
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getDataAddress()).isSameAs(dataAddress);
                var transferProcess = storedCaptor.getValue();
                assertThat(transferProcess.getState()).isEqualTo(STARTED.code());
                verify(dataAddressStore).store(dataAddress, transferProcess);
            });
        }

    }

    @Nested
    class StartupRequested {

        @Test
        void shouldNotifyDataPlaneAndTransitionToStarted() {
            var process = createTransferProcess(STARTUP_REQUESTED).toBuilder().type(CONSUMER).build();
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(STARTUP_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process);
            when(dataFlowController.started(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(dataFlowController).started(process);
                verify(transferProcessStore).save(captor.capture());
                assertThat(captor.getValue().stateAsString()).isEqualTo(STARTED.name());
                verify(listener).started(same(process), any());
            });
        }
    }

    @Nested
    class ResumingProvider {

        @Test
        void shouldStartDataTransferAndSendMessageToConsumer() {
            var process = createTransferProcess(RESUMING).toBuilder().type(PROVIDER).build();
            var dataFlowResponse = dataFlowResponseBuilder().build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), providerStateIs(RESUMING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process);
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
            when(dispatcherRegistry.dispatch(any(), any(), isA(TransferStartMessage.class))).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataAddressStore.store(any(), any())).thenReturn(StoreResult.success());

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferStartMessage.class);
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
                var transferCaptor = ArgumentCaptor.forClass(TransferProcess.class);
                verify(transferProcessStore).save(transferCaptor.capture());
                assertThat(transferCaptor.getValue().stateAsString()).isEqualTo(STARTED.name());
                verify(listener).started(eq(process), any());
                var message = captor.getValue();
                assertThat(message.getProcessId()).isEqualTo(process.getCorrelationId());
                assertThat(message.getConsumerPid()).isEqualTo(process.getCorrelationId());
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getDataAddress()).usingRecursiveComparison().isEqualTo(dataFlowResponse.getDataAddress());
                verify(dataAddressStore).store(dataFlowResponse.getDataAddress(), process);
            });
        }
    }

    @Nested
    class ResumingConsumer {

        @Test
        void shouldSendMessageToProviderAndTransitionToResumed() {
            var process = createTransferProcess(RESUMING).toBuilder().type(CONSUMER).build();
            when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
            when(transferProcessStore.nextNotLeased(anyInt(), consumerStateIs(RESUMING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process);
            when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));

            manager.start();

            await().untilAsserted(() -> {
                var captor = ArgumentCaptor.forClass(TransferStartMessage.class);
                verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
                verify(transferProcessStore).save(argThat(p -> p.getState() == RESUMED.code()));
                var message = captor.getValue();
                assertThat(message.getProcessId()).isEqualTo(process.getCorrelationId());
                assertThat(message.getProviderPid()).isEqualTo(process.getCorrelationId());
                assertThat(message.getConsumerPid()).isEqualTo(process.getId());
            });
        }
    }

    @Nested
    class Suspending {

        @Test
        void provider_shouldSuspendDataFlowAndTransitionToSuspended_whenMessageSentCorrectly() {
            var process = createTransferProcessBuilder(SUSPENDING).type(PROVIDER).correlationId("counterPartyId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(SUSPENDING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(SUSPENDING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).suspend(process);
                var captor = ArgumentCaptor.forClass(TransferSuspensionMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo(process.getId());
                assertThat(message.getConsumerPid()).isEqualTo("counterPartyId");
                assertThat(message.getProcessId()).isEqualTo("counterPartyId");
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == SUSPENDED.code()));
                verify(listener).suspended(process);
            });
        }

        @Test
        void consumer_shouldSuspendDataFlowAndTransitionToSuspended_whenMessageSentCorrectly() {
            var process = createTransferProcessBuilder(SUSPENDING).type(CONSUMER).correlationId("counterPartyId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(SUSPENDING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(SUSPENDING.code()).build());
            when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).suspend(process);
                var captor = ArgumentCaptor.forClass(TransferSuspensionMessage.class);
                verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), eq(Object.class), captor.capture());
                var message = captor.getValue();
                assertThat(message.getProviderPid()).isEqualTo("counterPartyId");
                assertThat(message.getConsumerPid()).isEqualTo(process.getId());
                assertThat(message.getProcessId()).isEqualTo("counterPartyId");
                verify(transferProcessStore).save(argThat(p -> p.getState() == SUSPENDED.code()));
                verify(listener).suspended(process);
            });
        }

    }

    @Nested
    class SuspendingRequestedProvider {

        @Test
        void shouldSuspendDataFlowAndTransitionToSuspendedAndNotSendMessage_whenMessageWasSentByCounterPart() {
            var process = createTransferProcessBuilder(SUSPENDING_REQUESTED).type(PROVIDER).correlationId("counterPartyId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(SUSPENDING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(SUSPENDING_REQUESTED.code()).build());
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).suspend(process);
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == SUSPENDED.code()));
                verify(listener).suspended(process);
            });
        }

        @Test
        void shouldTransitionToSuspendingRequested_whenDataFlowSuspensionFails() {
            var process = createTransferProcessBuilder(SUSPENDING_REQUESTED).type(PROVIDER).correlationId("counterPartyId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(SUSPENDING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(SUSPENDING_REQUESTED.code()).build());
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.failure(ERROR_RETRY));

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).suspend(process);
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == SUSPENDING_REQUESTED.code()));
            });
        }

    }

    @Nested
    class SuspendingRequestedConsumer {

        @Test
        void shouldSuspendDataFlowAndTransitionToSuspendedAndNotSendMessage_whenMessageWasSentByCounterPart() {
            var process = createTransferProcessBuilder(SUSPENDING_REQUESTED).type(CONSUMER).correlationId("counterPartyId").build();
            when(transferProcessStore.nextNotLeased(anyInt(), stateIs(SUSPENDING_REQUESTED.code()))).thenReturn(List.of(process)).thenReturn(emptyList());
            when(transferProcessStore.findById(process.getId())).thenReturn(process, process.toBuilder().state(SUSPENDING_REQUESTED.code()).build());
            when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());

            manager.start();

            await().untilAsserted(() -> {
                verify(dataFlowController).suspend(process);
                verifyNoInteractions(dispatcherRegistry);
                verify(transferProcessStore, atLeastOnce()).save(argThat(p -> p.getState() == SUSPENDED.code()));
                verify(listener).suspended(process);
            });
        }

    }

    private Criterion[] consumerStateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), criterion("type", "=", CONSUMER.name())});
    }

    private Criterion[] providerStateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), criterion("type", "=", PROVIDER.name())});
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending()});
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcessBuilder(inState).build();
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates state) {
        var processId = UUID.randomUUID().toString();

        return TransferProcess.Builder.newInstance()
                .type(CONSUMER)
                .id("test-process-" + processId)
                .state(state.code())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyAddress("http://an/address")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type(DESTINATION_TYPE).build())
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .protocol("protocol");
    }

    private static class DispatchFailureArguments implements ArgumentsProvider {

        private static final int RETRIES_NOT_EXHAUSTED = RETRY_LIMIT;
        private static final int RETRIES_EXHAUSTED = RETRIES_NOT_EXHAUSTED + 1;

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            CompletableFuture<StatusResult<Object>> genericError = failedFuture(new EdcException("error"));
            var fatalError = completedFuture(StatusResult.failure(FATAL_ERROR));
            return Stream.of(
                    // retries not exhausted
                    new DispatchFailure(REQUESTING, REQUESTING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(STARTING, STARTING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED).type(PROVIDER)),
                    new DispatchFailure(COMPLETING, COMPLETING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(SUSPENDING, SUSPENDING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(RESUMING, RESUMING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED).type(PROVIDER)),
                    new DispatchFailure(RESUMING, RESUMING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED).type(CONSUMER)),
                    new DispatchFailure(TERMINATING, TERMINATING, genericError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    // retries exhausted
                    new DispatchFailure(REQUESTING, TERMINATED, genericError, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(STARTING, TERMINATING, genericError, b -> b.stateCount(RETRIES_EXHAUSTED).type(PROVIDER)),
                    new DispatchFailure(COMPLETING, TERMINATING, genericError, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(SUSPENDING, TERMINATING, genericError, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(RESUMING, TERMINATING, genericError, b -> b.stateCount(RETRIES_EXHAUSTED).type(CONSUMER)),
                    new DispatchFailure(RESUMING, TERMINATING, genericError, b -> b.stateCount(RETRIES_EXHAUSTED).type(PROVIDER)),
                    new DispatchFailure(TERMINATING, TERMINATED, genericError, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    // fatal error, in this case retry should never be done
                    new DispatchFailure(REQUESTING, TERMINATED, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(STARTING, TERMINATING, fatalError, b -> b.type(PROVIDER).stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, TERMINATING, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(SUSPENDING, TERMINATING, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(RESUMING, TERMINATING, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED).type(CONSUMER)),
                    new DispatchFailure(RESUMING, TERMINATING, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED).type(PROVIDER)),
                    new DispatchFailure(TERMINATING, TERMINATED, fatalError, b -> b.stateCount(RETRIES_NOT_EXHAUSTED))
            );
        }
    }

}
