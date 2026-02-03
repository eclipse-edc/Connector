/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.process;

import org.eclipse.edc.connector.controlplane.defaults.storage.transferprocess.InMemoryTransferProcessStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.comparable;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ComponentTest
class TransferProcessManagerImplIntegrationTest {

    private static final int TRANSFER_MANAGER_BATCH_SIZE = 10;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final Clock clock = Clock.systemUTC();
    private final TransferProcessStore store = new InMemoryTransferProcessStore(clock, CriterionOperatorRegistryImpl.ofDefaults());
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final DataFlowController dataFlowController = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private final DataAddressStore dataAddressStore = mock();
    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> "any");

        var policyArchive = mock(PolicyArchive.class);
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(policyArchive.getAgreementIdForContract(anyString())).thenReturn("agreementId");

        var monitor = mock(Monitor.class);
        var waitStrategy = mock(ExponentialWaitStrategy.class);
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .dataFlowController(dataFlowController)
                .waitStrategy(waitStrategy)
                .batchSize(TRANSFER_MANAGER_BATCH_SIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(monitor)
                .clock(clock)
                .observable(mock())
                .store(store)
                .policyArchive(policyArchive)
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .addressResolver(mock())
                .dataAddressStore(dataAddressStore)
                .build();
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() {
        var numProcesses = TRANSFER_MANAGER_BATCH_SIZE * 2;
        when(dataFlowController.prepare(any(), any())).thenReturn(StatusResult.failure(FATAL_ERROR));

        var callback = CallbackAddress.Builder.newInstance().uri("local://test").build();
        var processes = IntStream.range(0, numProcesses)
                .mapToObj(num -> transferProcessBuilder().callbackAddresses(List.of(callback)).build())
                .peek(store::save)
                .collect(Collectors.toList());

        manager.start();

        await().untilAsserted(() -> {
            assertThat(processes).describedAs("All transfer processes state should be greater than INITIAL")
                    .allSatisfy(process -> {
                        var id = process.getId();
                        var storedProcess = store.findById(id);

                        assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore")
                                .isNotNull().extracting(StatefulEntity::getState).asInstanceOf(comparable(Integer.class))
                                .isGreaterThan(INITIAL.code());

                        assertThat(storedProcess.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
                    });
        });

    }

    private TransferProcess.Builder transferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .type(CONSUMER)
                .id("test-process-" + UUID.randomUUID())
                .correlationId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .contractId(UUID.randomUUID().toString());
    }

    @Nested
    class IdempotencyProcessStateReplication {

        @ParameterizedTest
        @ArgumentsSource(EgressMessages.class)
        void shouldSentMessageWithTheSameId_whenFirstDispatchFailed(TransferProcess.Type type, TransferProcessStates state,
                                                                    Class<? extends TransferRemoteMessage> messageType) {
            when(dispatcherRegistry.dispatch(any(), any(), isA(messageType)))
                    .thenReturn(completedFuture(StatusResult.failure(ERROR_RETRY)))
                    .thenReturn(completedFuture(StatusResult.success(TransferProcessAck.Builder.newInstance().build())));
            when(dataFlowController.start(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            when(dataFlowController.terminate(any())).thenReturn(StatusResult.success());
            when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("any"));

            var transfer = transferProcessBuilder().type(type).state(state.code()).build();
            store.save(transfer);

            manager.start();

            var sentMessages = ArgumentCaptor.forClass(messageType);
            await().atMost(TIMEOUT).untilAsserted(() -> {
                verify(dispatcherRegistry, times(2)).dispatch(any(), any(), sentMessages.capture());
                assertThat(sentMessages.getAllValues())
                        .map(TransferRemoteMessage::getId)
                        .matches(ids -> ids.stream().distinct().count() == 1);
            });

            await().atMost(TIMEOUT).untilAsserted(() -> {
                var actual = store.findById(transfer.getId());
                assertThat(actual).isNotNull();
                assertThat(actual.getState()).isNotEqualTo(state.code());
                assertThat(actual.lastSentProtocolMessage()).isNull();
            });
        }

        private static class EgressMessages implements ArgumentsProvider {

            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
                return Stream.of(
                        arguments(CONSUMER, REQUESTING, TransferRequestMessage.class),
                        arguments(CONSUMER, COMPLETING, TransferCompletionMessage.class),
                        arguments(CONSUMER, TERMINATING, TransferTerminationMessage.class),
                        arguments(PROVIDER, STARTING, TransferStartMessage.class),
                        arguments(PROVIDER, COMPLETING, TransferCompletionMessage.class),
                        arguments(PROVIDER, TERMINATING, TransferTerminationMessage.class)
                );
            }
        }
    }
}

