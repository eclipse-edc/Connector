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
 *
 */

package org.eclipse.edc.connector.transfer.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.TestProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.TestResourceDefinition;
import org.eclipse.edc.connector.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.transfer.process.TransferProcessManagerImpl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.SecretToken;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.connector.transfer.spi.types.command.TransferProcessCommand;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class TransferProcessManagerImplTest {

    private static final String DESTINATION_TYPE = "test-type";
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private static final String PROVISIONED_RESOURCE_ID = "1";
    private static final int RETRY_LIMIT = 1;
    private static final int RETRY_EXHAUSTED = RETRY_LIMIT + 1;

    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore transferProcessStore = mock(TransferProcessStore.class);
    private final PolicyArchive policyArchive = mock(PolicyArchive.class);
    private final DataFlowManager dataFlowManager = mock(DataFlowManager.class);
    private final Vault vault = mock(Vault.class);
    private final Clock clock = Clock.systemUTC();
    private final TransferProcessListener listener = mock(TransferProcessListener.class);
    private final CommandQueue<TransferProcessCommand> commandQueue = mock(CommandQueue.class);
    private final CommandRunner<TransferProcessCommand> commandRunner = mock(CommandRunner.class);

    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success(createDataFlowResponse()));
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        var entityRetryProcessConfiguration = new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L));
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(() -> 50L)
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .commandQueue(commandQueue)
                .commandRunner(commandRunner)
                .typeManager(new TypeManager())
                .clock(clock)
                .statusCheckerRegistry(statusCheckerRegistry)
                .observable(observable)
                .transferProcessStore(transferProcessStore)
                .policyArchive(policyArchive)
                .vault(vault)
                .addressResolver(mock(DataAddressResolver.class))
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .build();
    }

    @Test
    void verifyCallbacks() {
        when(transferProcessStore.processIdForDataRequestId("1")).thenReturn(null, "2");
        var callback = CallbackAddress.Builder.newInstance().uri("local://test").events(Set.of("test")).build();
        var dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .callbackAddresses(List.of(callback))
                .build();

        var captor = ArgumentCaptor.forClass(TransferProcess.class);

        manager.start();
        manager.initiateConsumerRequest(transferRequest);
        manager.stop();

        verify(transferProcessStore, times(RETRY_LIMIT)).save(captor.capture());
        assertThat(captor.getValue().getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
        verify(listener).initiated(any());

    }

    @Test
    void initial_shouldTransitionToProvisioning() {
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(INITIAL.code()), anyInt()))
                .thenReturn(List.of(createTransferProcess(INITIAL)))
                .thenReturn(emptyList());
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class)))
                .thenReturn(Result.success(resourceManifest));

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verifyNoInteractions(provisionManager);
            verify(transferProcessStore).save(argThat(p -> p.getState() == PROVISIONING.code()));
        });
    }

    @Test
    void initial_manifestEvaluationFailed_shouldTransitionToTerminating() {
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(INITIAL.code()), anyInt()))
                .thenReturn(List.of(createTransferProcess(INITIAL)))
                .thenReturn(emptyList());
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class)))
                .thenReturn(Result.failure("error"));

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verifyNoInteractions(provisionManager);
            verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnDataDestination() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance()
                .resource(provisionedDataDestinationResource())
                .build());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == PROVISIONED.code()));
            verify(listener).provisioned(process);
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnContentAddress() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();

        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance()
                .resource(createTestProvisionedContentResource("3"))
                .secretToken(new TestToken())
                .build());

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == PROVISIONED.code()));
            verify(vault).storeSecret(any(), any());
            verify(listener).provisioned(process);
        });
    }

    @Test
    void provisioning_shouldTransitionToProvisioningRequestedOnResponseStarted() {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();
        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance().inProcess(true).build());

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == PROVISIONING_REQUESTED.code()));
            verify(listener).provisioningRequested(any());
        });
    }

    @Test
    void provisioning_shouldTransitionToTerminating_whenProvisionErrorAndRetriesExhausted() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .stateCount(2)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void provisioning_shouldTransitionToTerminatingOnFatalProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(ResponseStatus.FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void provisioning_shouldContinueOnRetryProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(ResponseStatus.ERROR_RETRY, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == PROVISIONING.code()));
        });
    }

    @Test
    void provisionedConsumer_shouldTransitionToRequesting() {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(CONSUMER).build();
        when(transferProcessStore.nextForState(eq(PROVISIONED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, atLeastOnce()).nextForState(eq(PROVISIONED.code()), anyInt());
            verify(transferProcessStore).save(argThat(p -> p.getState() == REQUESTING.code()));
        });
    }

    @Test
    void provisionedProvider_shouldTransitionToStarting() {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(PROVIDER).build();
        when(transferProcessStore.nextForState(eq(PROVISIONED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void requesting_shouldSendMessageAndTransitionToRequested() {
        var process = createTransferProcess(REQUESTING);
        when(dispatcherRegistry.send(eq(Object.class), any())).thenReturn(completedFuture("any"));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(eq(Object.class), isA(TransferRequestMessage.class));
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == REQUESTED.code()));
            verify(listener).requested(process);
        });
    }

    @Test
    @Deprecated(since = "milestone9")
    void requested_shouldDoNothing_dataspaceProtocol() {
        var dataRequest = createDataRequestBuilder().protocol("dataspace").build();
        var process = createTransferProcessBuilder(REQUESTED)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(provisionedDataDestinationResource()))
                        .build())
                .dataRequest(dataRequest).build();
        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().pollDelay(RETRY_LIMIT, SECONDS).untilAsserted(() -> verify(transferProcessStore, never()).save(any()));
    }

    @Test
    @Deprecated(since = "milestone9")
    void requested_shouldTransitionToStarting_idsMultipartOnly() {
        var dataRequest = createDataRequestBuilder().protocol("ids-multipart").build();
        var process = createTransferProcessBuilder(REQUESTED)
                .type(CONSUMER)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(provisionedDataDestinationResource()))
                        .build())
                .dataRequest(dataRequest).build();
        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTED.code()));
        });
    }

    @Test
    @Deprecated(since = "milestone9")
    void requested_shouldNotTransitionIfProvisionedResourcesAreEmpty_idsMultipartOnly() {
        var dataRequest = createDataRequestBuilder().protocol("ids-multipart").build();
        var process = createTransferProcessBuilder(REQUESTED)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .dataRequest(dataRequest).build();
        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenReturn(List.of(process));
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).save(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, never()).save(any());
        });
    }

    @Test
    void starting_shouldStartDataTransferAndSendMessageToConsumer() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        var dataFlowResponse = createDataFlowResponse();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success(dataFlowResponse));
        when(dispatcherRegistry.send(any(), isA(TransferStartMessage.class))).thenReturn(completedFuture("any"));

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(TransferStartMessage.class);
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(dispatcherRegistry).send(any(), captor.capture());
            assertThat(captor.getValue().getDataAddress()).usingRecursiveComparison().isEqualTo(dataFlowResponse.getDataAddress());
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTED.code()));
            verify(listener).started(eq(process), any());
        });
    }

    @Test
    void starting_onFailureAndRetriesNotExhausted_updatesStateCountForRetry() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void starting_shouldTransitionToTerminatingIfFatalFailure() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void starting_onFailureAndRetriesExhausted_transitToTerminating() {
        var process = createTransferProcessBuilder(STARTING).type(PROVIDER).stateCount(RETRY_EXHAUSTED).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void starting_whenShouldWait_updatesStateCount() {
        var process = createTransferProcessBuilder(STARTING).type(PROVIDER).stateCount(2).stateTimestamp(clock.millis() + 1000L).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dataFlowManager);
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTING.code()));
        });
    }

    @Test
    void started_shouldComplete_whenManagedResourcesAndCheckerCompleted() {
        var process = createTransferProcess(STARTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        when(transferProcessStore.nextForState(eq(STARTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((tp, resources) -> true);

        manager.start();

        await().untilAsserted(() -> {
            verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
            verify(transferProcessStore).save(argThat(p -> p.getState() == COMPLETING.code()));
        });
    }

    @Test
    void started_shouldComplete_whenNotManagedResourcesAndCheckerCompleted() {
        var process = createTransferProcessBuilder(STARTED, false)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(provisionedDataDestinationResource(), provisionedDataDestinationResource()))
                        .build())
                .build();

        when(transferProcessStore.nextForState(eq(STARTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((tp, resources) -> true);

        manager.start();

        await().untilAsserted(() -> {
            verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
            verify(transferProcessStore).save(argThat(p -> p.getState() == COMPLETING.code()));
        });
    }

    @Test
    void started_shouldBreakLeaseAndNotComplete_whenNotAllYetCompleted() {
        var process = createTransferProcess(STARTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        when(transferProcessStore.nextForState(eq(STARTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((tp, resources) -> false);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTED.code()));
        });
    }

    @Test
    void started_shouldNotComplete_whenNoCheckerForManaged() {
        var process = createTransferProcess(STARTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        when(transferProcessStore.nextForState(eq(STARTED.code()), anyInt())).thenReturn(List.of(process));
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).save(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, never()).save(any());
        });
    }

    @Test
    void started_shouldComplete_whenNoCheckerForNotManaged() {
        var process = createTransferProcessBuilder(STARTED, false)
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance()
                        .resources(List.of(provisionedDataDestinationResource(), provisionedDataDestinationResource()))
                        .build())
                .build();

        when(transferProcessStore.nextForState(eq(STARTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn(null);

        manager.start();

        await().untilAsserted(() -> {
            verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
            verify(transferProcessStore).save(argThat(p -> p.getState() == COMPLETING.code()));
        });
    }

    @Test
    void completing_shouldTransitionToCompleted_whenSendingMessageSucceed() {
        var process = createTransferProcess(COMPLETING);
        when(transferProcessStore.nextForState(eq(COMPLETING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING.code()).build());
        when(dispatcherRegistry.send(any(), isA(TransferCompletionMessage.class))).thenReturn(completedFuture("any"));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(any(), isA(TransferCompletionMessage.class));
            verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == COMPLETED.code()));
            verify(listener).completed(process);
        });
    }

    @Test
    void terminating_shouldTransitionToTerminated_whenMessageSentCorrectly() {
        var process = createTransferProcessBuilder(TERMINATING).type(PROVIDER).build();
        when(transferProcessStore.nextForState(eq(TERMINATING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());
        when(dispatcherRegistry.send(any(), isA(TransferTerminationMessage.class))).thenReturn(completedFuture("any"));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(any(), isA(TransferTerminationMessage.class));
            verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
        });
    }

    @Test
    void terminating_shouldTransitionToTerminatedWithoutSendingMessage_whenConsumerAndIsNotRequestedYet() {
        var process = createTransferProcessBuilder(TERMINATING).type(CONSUMER).state(REQUESTING.code()).build();
        when(transferProcessStore.nextForState(eq(TERMINATING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dispatcherRegistry);
            verify(transferProcessStore, times(RETRY_LIMIT)).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(listener).terminated(process);
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisioned() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TokenTestProvisionResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.success(DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId(PROVISIONED_RESOURCE_ID)
                .build());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).find(process.getId());
            verify(transferProcessStore).save(argThat(p -> p.getState() == DEPROVISIONED.code()));
            verify(vault).deleteSecret(any());
            verify(listener).deprovisioned(process);
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisioningRequestedOnResponseStarted() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TokenTestProvisionResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionedResponse = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("any")
                .inProcess(true)
                .build();

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(StatusResult.success(deprovisionedResponse))));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == DEPROVISIONING_REQUESTED.code()));
            verify(listener).deprovisioningRequested(any());
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisionedWithErrorOnFatalDeprovisionError() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.<DeprovisionedResource>failure(ResponseStatus.FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == DEPROVISIONED.code() && p.getErrorDetail() != null));
            verify(listener).deprovisioned(process);
        });
    }

    @Test
    void deprovisioning_shouldNotTransitionOnRetriableDeprovisionError() {
        var manifest = ResourceManifest.Builder.newInstance()
                .definitions(List.of(new TestResourceDefinition()))
                .build();

        var resourceSet = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(new TestProvisionedDataDestinationResource("test", PROVISIONED_RESOURCE_ID)))
                .build();

        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(manifest)
                .provisionedResourceSet(resourceSet)
                .build();

        var deprovisionResult = StatusResult.<DeprovisionedResource>failure(ResponseStatus.ERROR_RETRY, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(deprovisionResult)));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == DEPROVISIONING.code()));
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        });
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisionedWithErrorOnDeprovisionFailure() {
        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .stateCount(RETRY_EXHAUSTED)
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(transferProcessStore).save(argThat(p -> p.getState() == DEPROVISIONED.code() && p.getErrorDetail() != null));
            verify(listener).deprovisioned(process);
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DispatchFailureArguments.class)
    void dispatchFailure(TransferProcessStates starting, TransferProcessStates ending, UnaryOperator<TransferProcess.Builder> builderEnricher) {
        var negotiation = builderEnricher.apply(createTransferProcessBuilder(starting).state(starting.code())).build();
        when(transferProcessStore.nextForState(eq(starting.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(transferProcessStore.find(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore).save(argThat(p -> p.getState() == ending.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
        });
    }

    @Test
    void enqueueCommand_willEnqueueCommandOnCommandQueue() {
        var command = new TransferProcessCommand() {
        };

        manager.enqueueCommand(command);

        verify(commandQueue).enqueue(command);
    }

    @Test
    void runCommand_willRunCommandAndReturnResult() {
        var command = new TransferProcessCommand() {
        };
        when(commandRunner.runCommand(command)).thenReturn(Result.success());

        var result = manager.runCommand(command);

        assertThat(result).matches(Result::succeeded);
        verify(commandRunner).runCommand(command);
    }

    private TestProvisionedContentResource createTestProvisionedContentResource(String resourceDefinitionId) {
        return TestProvisionedContentResource.Builder.newInstance()
                .resourceName("test")
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId(resourceDefinitionId)
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .hasToken(true)
                .build();
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcessBuilder(inState, true).build();
    }

    private DataFlowResponse createDataFlowResponse() {
        return DataFlowResponse.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .build();
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState) {
        return createTransferProcessBuilder(inState, true);
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState, boolean managed) {
        var processId = UUID.randomUUID().toString();
        var dataRequest = createDataRequestBuilder()
                .processId(processId)
                .transferType(new TransferType())
                .protocol("ids-protocol")
                .connectorAddress("http://an/address")
                .managedResources(managed)
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(CONSUMER)
                .id("test-process-" + processId)
                .state(inState.code())
                .dataRequest(dataRequest);
    }

    private DataRequest.Builder createDataRequestBuilder() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .destinationType(DESTINATION_TYPE);
    }

    private ProvisionedDataDestinationResource provisionedDataDestinationResource() {
        return new TestProvisionedDataDestinationResource("test-resource", PROVISIONED_RESOURCE_ID);
    }

    private static class DispatchFailureArguments implements ArgumentsProvider {

        private static final int RETRIES_NOT_EXHAUSTED = RETRY_LIMIT;
        private static final int RETRIES_EXHAUSTED = RETRIES_NOT_EXHAUSTED + 1;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // retries not exhausted
                    new DispatchFailure(REQUESTING, REQUESTING, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(STARTING, STARTING, b -> b.type(PROVIDER).stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, COMPLETING, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    new DispatchFailure(TERMINATING, TERMINATING, b -> b.stateCount(RETRIES_NOT_EXHAUSTED)),
                    // retries exhausted
                    new DispatchFailure(REQUESTING, TERMINATING, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(STARTING, TERMINATING, b -> b.type(PROVIDER).stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(COMPLETING, TERMINATING, b -> b.stateCount(RETRIES_EXHAUSTED)),
                    new DispatchFailure(TERMINATING, TERMINATED, b -> b.stateCount(RETRIES_EXHAUSTED))
            );
        }

    }


    @JsonTypeName("dataspaceconnector:testprovisioneddcontentresource")
    @JsonDeserialize(builder = TestProvisionedContentResource.Builder.class)
    private static class TestProvisionedContentResource extends ProvisionedContentResource {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProvisionedContentResource.Builder<TestProvisionedContentResource, Builder> {

            protected Builder() {
                super(new TestProvisionedContentResource());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }

    private static class TokenTestProvisionResource extends TestProvisionedDataDestinationResource {
        TokenTestProvisionResource(String resourceName, String id) {
            super(resourceName, id);
            hasToken = true;
        }
    }

    private static class TestToken implements SecretToken {

        @Override
        public long getExpiration() {
            return 0;
        }

    }

}
