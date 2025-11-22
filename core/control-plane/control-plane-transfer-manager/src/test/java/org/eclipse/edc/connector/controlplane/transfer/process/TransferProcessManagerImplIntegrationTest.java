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
import org.eclipse.edc.connector.controlplane.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.provision.fixtures.TestResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
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
import org.eclipse.edc.spi.result.Result;
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
    private final ProvisionManager provisionManager = mock();
    private final ResourceManifestGenerator manifestGenerator = mock();
    private final Clock clock = Clock.systemUTC();
    private final TransferProcessStore store = new InMemoryTransferProcessStore(clock, CriterionOperatorRegistryImpl.ofDefaults());
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final DataFlowManager dataFlowManager = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> "any");
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerResourceManifest(any(TransferProcess.class), any(Policy.class))).thenReturn(Result.success(resourceManifest));

        var policyArchive = mock(PolicyArchive.class);
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(policyArchive.getAgreementIdForContract(anyString())).thenReturn("agreementId");

        var monitor = mock(Monitor.class);
        var waitStrategy = mock(ExponentialWaitStrategy.class);
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(waitStrategy)
                .batchSize(TRANSFER_MANAGER_BATCH_SIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(monitor)
                .clock(clock)
                .observable(mock())
                .store(store)
                .policyArchive(policyArchive)
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .addressResolver(mock())
                .provisionResponsesHandler(new ProvisionResponsesHandler(mock(), mock(), mock(), mock()))
                .deprovisionResponsesHandler(mock())
                .build();
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() {
        var numProcesses = TRANSFER_MANAGER_BATCH_SIZE * 2;
        when(dataFlowManager.prepare(any(), any())).thenReturn(StatusResult.failure(FATAL_ERROR));
        when(provisionManager.provision(any(), any(Policy.class))).thenAnswer(i -> completedFuture(List.of(
                ProvisionResponse.Builder.newInstance()
                        .resource(new TestProvisionedDataDestinationResource("any", "1"))
                        .build()
        )));

        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").build();
        var processes = IntStream.range(0, numProcesses)
                .mapToObj(i -> provisionedResourceSet())
                .map(resourceSet -> transferProcessBuilder().resourceManifest(manifest).callbackAddresses(List.of(callback)).provisionedResourceSet(resourceSet).build())
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
            verify(provisionManager, times(numProcesses)).provision(any(), any());
        });

    }

    private ProvisionedResourceSet provisionedResourceSet() {
        return ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test-resource", "1")))
                .build();
    }

    private TransferProcess.Builder transferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
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
            when(dataFlowManager.start(any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
            when(dataFlowManager.terminate(any())).thenReturn(StatusResult.success());

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

