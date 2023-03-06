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
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
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
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessManagerImplTest {

    private static final String DESTINATION_TYPE = "test-type";
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private static final String PROVISIONED_RESOURCE_ID = "1";

    private final long currentTime = 1343411;

    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore transferProcessStore = mock(TransferProcessStore.class);
    private final PolicyArchive policyArchive = mock(PolicyArchive.class);
    private final DataFlowManager dataFlowManager = mock(DataFlowManager.class);
    private final Vault vault = mock(Vault.class);
    private final SendRetryManager sendRetryManager = mock(SendRetryManager.class);
    private final TransferProcessListener listener = mock(TransferProcessListener.class);

    private TransferProcessManagerImpl manager;

    @BeforeEach
    void setup() {
        var observable = new TransferProcessObservableImpl();
        observable.registerListener(listener);
        manager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(mock(ExponentialWaitStrategy.class))
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .commandQueue(mock(CommandQueue.class))
                .commandRunner(mock(CommandRunner.class))
                .typeManager(new TypeManager())
                .clock(Clock.fixed(Instant.ofEpochMilli(currentTime), UTC))
                .statusCheckerRegistry(statusCheckerRegistry)
                .observable(observable)
                .transferProcessStore(transferProcessStore)
                .policyArchive(policyArchive)
                .vault(vault)
                .addressResolver(mock(DataAddressResolver.class))
                .sendRetryManager(sendRetryManager)
                .build();
    }

    /**
     * All creations operations must be idempotent in order to support reliability (e.g. messages/requests may be delivered more than once).
     */
    @Test
    void verifyIdempotency() {
        when(transferProcessStore.processIdForDataRequestId("1")).thenReturn(null, "2");
        var dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        manager.start();
        manager.initiateProviderRequest(dataRequest);
        manager.initiateProviderRequest(dataRequest); // repeat request
        manager.stop();

        verify(transferProcessStore, times(1)).save(isA(TransferProcess.class));
        verify(transferProcessStore, times(2)).processIdForDataRequestId(anyString());
    }

    @Test
    void verifyCreatedTimestamp() {
        when(transferProcessStore.processIdForDataRequestId("1")).thenReturn(null, "2");
        var dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        manager.start();
        manager.initiateProviderRequest(dataRequest);
        manager.stop();

        verify(transferProcessStore, times(1)).save(argThat(p -> p.getCreatedAt() == currentTime));
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
            verify(transferProcessStore).find(process.getId());
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
    void provisioning_shouldTransitionToTerminatingOnProvisionError() {
        var process = createTransferProcess(PROVISIONING).toBuilder()
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
    void requesting_shouldTransitionToRequested() {
        var process = createTransferProcess(REQUESTING);
        when(dispatcherRegistry.send(eq(Object.class), any())).thenReturn(completedFuture("any"));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == REQUESTED.code()));
            verify(listener).requested(process);
        });
    }

    @Test
    void requesting_OnFailureAndRetriesNotExhausted_updatesStateCountForRetry() {
        var process = createTransferProcess(REQUESTING);
        when(dispatcherRegistry.send(eq(Object.class), any())).thenReturn(failedFuture(new EdcException("send failed")));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == REQUESTING.code()));
        });
    }

    @Test
    void requesting_onFailureAndRetriesExhausted_transitToTerminating() {
        var process = createTransferProcess(REQUESTING);
        when(dispatcherRegistry.send(eq(Object.class), any())).thenReturn(failedFuture(new EdcException("send failed")));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
        when(sendRetryManager.retriesExhausted(process)).thenReturn(true);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void requesting_whenShouldWait_updatesStateCount() {
        var process = createTransferProcess(REQUESTING);
        when(sendRetryManager.shouldDelay(process)).thenReturn(true);
        when(dispatcherRegistry.send(eq(Object.class), any())).thenReturn(completedFuture("any"));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dispatcherRegistry);
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == REQUESTING.code()));
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

        await().pollDelay(1, SECONDS).untilAsserted(() -> verify(transferProcessStore, never()).save(any()));
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
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success());
        when(dispatcherRegistry.send(any(), isA(TransferStartMessage.class), any())).thenReturn(completedFuture("any"));

        manager.start();

        await().untilAsserted(() -> {
            verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
            verify(dispatcherRegistry).send(any(), isA(TransferStartMessage.class), any());
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTED.code()));
        });
    }

    @Test
    void starting_shouldStartDataTransferAndNotTransitToStarted_whenMessageSendFailed() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success());
        when(dispatcherRegistry.send(any(), isA(TransferStartMessage.class), any())).thenReturn(failedFuture(new EdcException("error in sending the message")));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(any(), isA(TransferStartMessage.class), any());
            verify(transferProcessStore).save(argThat(p -> p.getState() == STARTING.code()));
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
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == STARTING.code()));
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
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());
        when(sendRetryManager.retriesExhausted(process)).thenReturn(true);

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == TERMINATING.code()));
        });
    }

    @Test
    void starting_whenShouldWait_updatesStateCount() {
        var process = createTransferProcess(STARTING).toBuilder().type(PROVIDER).build();
        when(sendRetryManager.shouldDelay(process)).thenReturn(true);
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.failure(ResponseStatus.ERROR_RETRY));
        when(transferProcessStore.nextForState(eq(STARTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(STARTING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verifyNoInteractions(dataFlowManager);
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == STARTING.code()));
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
        var process = createTransferProcess(STARTED, new TransferType(), false);
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
        var process = createTransferProcess(STARTED, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

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
        when(dispatcherRegistry.send(any(), isA(TransferCompletionMessage.class), any())).thenReturn(completedFuture("any"));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(any(), isA(TransferCompletionMessage.class), any());
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == COMPLETED.code()));
            verify(listener).completed(process);
        });
    }

    @Test
    void completing_shouldNotTransitionToCompleted_whenSendingMessageFailed() {
        var process = createTransferProcess(COMPLETING);
        when(transferProcessStore.nextForState(eq(COMPLETING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(COMPLETING.code()).build());
        when(dispatcherRegistry.send(any(), isA(TransferCompletionMessage.class), any())).thenReturn(failedFuture(new EdcException("an error")));

        manager.start();

        await().untilAsserted(() -> {
            verify(dispatcherRegistry).send(any(), isA(TransferCompletionMessage.class), any());
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == COMPLETING.code()));
        });
    }

    @Test
    void terminating_shouldTransitionToTerminated() {
        var process = createTransferProcess(TERMINATING);
        when(transferProcessStore.nextForState(eq(TERMINATING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(TERMINATING.code()).build());

        manager.start();

        await().untilAsserted(() -> {
            verify(transferProcessStore, times(1)).save(argThat(p -> p.getState() == TERMINATED.code()));
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
        return createTransferProcessBuilder(inState, new TransferType(), true).build();
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type, boolean managed) {
        return createTransferProcessBuilder(inState, type, managed)
                .build();
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState) {
        return createTransferProcessBuilder(inState, new TransferType(), true);
    }

    private TransferProcess.Builder createTransferProcessBuilder(TransferProcessStates inState, TransferType type, boolean managed) {
        var processId = UUID.randomUUID().toString();
        var dataRequest = createDataRequestBuilder()
                .processId(processId)
                .transferType(type)
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
