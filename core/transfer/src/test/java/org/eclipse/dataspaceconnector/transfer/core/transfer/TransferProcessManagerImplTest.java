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

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.retry.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.core.TestProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ENDED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.STREAMING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessManagerImplTest {

    private static final String DESTINATION_TYPE = "test-type";
    private static final long TIMEOUT = 5;
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private static final String PROVISIONED_RESOURCE_ID = "1";

    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore transferProcessStore = mock(TransferProcessStore.class);
    private final PolicyArchive policyArchive = mock(PolicyArchive.class);
    private final DataFlowManager dataFlowManager = mock(DataFlowManager.class);
    private final Vault vault = mock(Vault.class);
    @SuppressWarnings("unchecked")
    private final SendRetryManager<TransferProcess> sendRetryManager = mock(SendRetryManager.class);

    private TransferProcessManagerImpl manager;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
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
                .statusCheckerRegistry(statusCheckerRegistry)
                .observable(mock(TransferProcessObservable.class))
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
        when(transferProcessStore.processIdForTransferId("1")).thenReturn(null, "2");
        DataRequest dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        manager.start();
        manager.initiateProviderRequest(dataRequest);
        manager.initiateProviderRequest(dataRequest); // repeat request
        manager.stop();

        verify(transferProcessStore, times(1)).create(isA(TransferProcess.class));
        verify(transferProcessStore, times(2)).processIdForTransferId(anyString());
    }

    @Test
    void initial_shouldTransitionToProvisioning() throws InterruptedException {
        var process = createTransferProcess(INITIAL);

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());

        when(transferProcessStore.nextForState(eq(INITIAL.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerResourceManifest(any(DataRequest.class), any(Policy.class))).thenReturn(resourceManifest);

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verifyNoInteractions(provisionManager);
        verify(transferProcessStore).update(argThat(p -> p.getState() == PROVISIONING.code()));
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnDataDestination() throws InterruptedException {
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
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == PROVISIONED.code()));
    }

    @Test
    void provisioning_shouldTransitionToProvisionedOnContentAddress() throws InterruptedException {
        var resourceDefinition = TestResourceDefinition.Builder.newInstance().id("3").build();

        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(resourceDefinition)).build())
                .build();

        var resource = TestProvisionedContentResource.Builder.newInstance()
                .resourceName("test")
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId("3")
                .dataAddress(DataAddress.Builder.newInstance().type("test").build())
                .hasToken(true)
                .build();

        var provisionResult = StatusResult.success(ProvisionResponse.Builder.newInstance()
                .resource(resource)
                .secretToken(new TestToken())
                .build());

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);


        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();

        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == PROVISIONED.code()));
        verify(vault).storeSecret(any(), any());
    }

    @Test
    void provisioning_shouldTransitionToErrorOnProvisionError() throws InterruptedException {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == ERROR.code()));
    }

    @Test
    void provisioning_shouldTransitionToErrorOnFatalProvisionError() throws InterruptedException {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(ResponseStatus.FATAL_ERROR, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == ERROR.code()));
    }

    @Test
    void provisioning_shouldContinueOnRetryProvisionError() throws InterruptedException {
        var process = createTransferProcess(PROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        var provisionResult = StatusResult.<ProvisionResponse>failure(ResponseStatus.ERROR_RETRY, "test error");

        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.provision(any(), isA(Policy.class))).thenReturn(completedFuture(List.of(provisionResult)));
        when(transferProcessStore.nextForState(eq(PROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == PROVISIONING.code()));
    }

    @Test
    void provisionedConsumer_shouldTransitionToRequesting() throws InterruptedException {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(CONSUMER).build();

        when(transferProcessStore.nextForState(eq(PROVISIONED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, atLeastOnce()).nextForState(eq(PROVISIONED.code()), anyInt());
        verify(transferProcessStore).update(argThat(p -> p.getState() == REQUESTING.code()));
    }

    @Test
    void provisionedProvider_shouldTransitionToInProgress() throws InterruptedException {
        var process = createTransferProcess(PROVISIONED).toBuilder().type(PROVIDER).build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(transferProcessStore.nextForState(eq(PROVISIONED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(dataFlowManager.initiate(any(), any(), any())).thenReturn(StatusResult.success("any"));
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == IN_PROGRESS.code()));
    }

    @Test
    void requesting_shouldTransitionToRequested() throws InterruptedException {
        var process = createTransferProcess(REQUESTING);
        var latch = countDownOnUpdateLatch(1);
        when(dispatcherRegistry.send(eq(Object.class), any(), any())).thenReturn(completedFuture("any"));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, times(1)).update(argThat(p -> p.getState() == REQUESTED.code()));
    }

    @Test
    void requesting_OnFailureAndRetriesNotExhausted_updatesStateCountForRetry() throws InterruptedException {
        var process = createTransferProcess(REQUESTING);
        var latch = countDownOnUpdateLatch(1);
        when(dispatcherRegistry.send(eq(Object.class), any(), any())).thenReturn(failedFuture(new EdcException("send failed")));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, times(1)).update(argThat(p -> p.getState() == REQUESTING.code()));
    }

    @Test
    void requesting_OnFailureAndRetriesExhausted_updatesStateCountForRetry() throws InterruptedException {
        var process = createTransferProcess(REQUESTING);
        var latch = countDownOnUpdateLatch(1);
        when(dispatcherRegistry.send(eq(Object.class), any(), any())).thenReturn(failedFuture(new EdcException("send failed")));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());
        when(sendRetryManager.retriesExhausted(process)).thenReturn(true);

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, times(1)).update(argThat(p -> p.getState() == ERROR.code()));
    }

    @Test
    void requesting_whenShouldWait_updatesStateCount() throws InterruptedException {
        var process = createTransferProcess(REQUESTING);
        when(sendRetryManager.shouldDelay(process))
                .thenReturn(true);
        var latch = countDownOnUpdateLatch(1);
        when(dispatcherRegistry.send(eq(Object.class), any(), any())).thenReturn(completedFuture("any"));
        when(transferProcessStore.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process, process.toBuilder().state(REQUESTING.code()).build());

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verifyNoInteractions(dispatcherRegistry);
        verify(transferProcessStore, times(1)).update(argThat(p -> p.getState() == REQUESTING.code()));
    }

    @Test
    void requested_shouldTransitionToInProgressIfTransferIsFinite() throws InterruptedException {
        var process = createTransferProcess(REQUESTED);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore).update(argThat(p -> p.getState() == IN_PROGRESS.code()));
    }

    @Test
    void requested_shouldTransitionToStreamingIfTransferIsNonFinite() throws InterruptedException {
        var nonFinite = TransferType.Builder.transferType().isFinite(false).build();
        var process = createTransferProcess(REQUESTED, nonFinite, true);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        var latch = countDownOnUpdateLatch();

        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore).update(argThat(p -> p.getState() == STREAMING.code()));
    }

    @Test
    void requested_shouldNotTransitionIfProvisionedResourcesAreEmpty() throws InterruptedException {
        var process = createTransferProcess(REQUESTED);

        var latch = new CountDownLatch(1);

        when(transferProcessStore.nextForState(eq(REQUESTED.code()), anyInt())).thenAnswer(i -> {
            latch.countDown();
            return List.of(process);
        });
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).update(process);

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, never()).update(any());
    }

    @Test
    @DisplayName("checkComplete: should transition process with managed resources if checker returns completed")
    void verifyCompletedManagedResources() throws InterruptedException {
        var process = createTransferProcess(IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        when(transferProcessStore.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(transferProcessStore).update(argThat(p -> p.getState() == COMPLETED.code()));
    }

    @Test
    @DisplayName("checkComplete: should transition process with no managed resources if checker returns completed")
    void verifyCompletedNonManagedResources() throws InterruptedException {
        TransferProcess process = createTransferProcess(REQUESTED, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        when(transferProcessStore.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(transferProcessStore).update(argThat(p -> p.getState() == COMPLETED.code()));
    }

    @Test
    @DisplayName("checkComplete: should not transition process if checker returns not yet completed")
    void verifyCompleted_notAllYetCompleted() throws InterruptedException {
        var process = createTransferProcess(IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        var latch = new CountDownLatch(1);

        when(transferProcessStore.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenAnswer(i -> {
            latch.countDown();
            return List.of(process);
        });
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).update(process);

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(transferProcessStore, never()).update(any());
    }

    @Test
    @DisplayName("checkComplete: should not transition process with managed resources but no status checker")
    void verifyCompleted_noCheckerForManaged() throws InterruptedException {
        var process = createTransferProcess(IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        var latch = new CountDownLatch(1);
        when(transferProcessStore.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenAnswer(i -> {
            latch.countDown();
            return List.of(process);
        });
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(transferProcessStore).update(process);

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore, never()).update(any());
    }

    @Test
    @DisplayName("checkComplete: should automatically transition process with no managed resources if no checker")
    void verifyCompleted_noCheckerForSomeResources() throws InterruptedException {
        var process = createTransferProcess(IN_PROGRESS, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());
        process.getProvisionedResourceSet().addResource(provisionedDataDestinationResource());

        var latch = countDownOnUpdateLatch();

        when(transferProcessStore.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(statusCheckerRegistry.resolve(anyString())).thenReturn(null);

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(transferProcessStore).update(argThat(p -> p.getState() == COMPLETED.code()));
    }

    @Test
    void deprovisioning_shouldTransitionToDeprovisioned() throws InterruptedException {
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

        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == DEPROVISIONED.code()));
        verify(vault).deleteSecret(any());
    }

    @Test
    void deprovisioning_shouldTransitionToErrorOnFatalDeprovisionError() throws InterruptedException {
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
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == ERROR.code()));
    }

    @Test
    void deprovisioning_shouldNotTransitionOnRetriableDeprovisionError() throws InterruptedException {
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
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore).update(argThat(p -> p.getState() == DEPROVISIONING.code()));
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
    }

    @Test
    void deprovisioning_shouldTransitionToErrorOnDeprovisionException() throws InterruptedException {
        var process = createTransferProcess(DEPROVISIONING).toBuilder()
                .resourceManifest(ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build())
                .build();
        when(policyArchive.findPolicyForContract(anyString())).thenReturn(Policy.Builder.newInstance().build());
        when(provisionManager.deprovision(any(), isA(Policy.class))).thenReturn(failedFuture(new EdcException("provision failed")));
        when(transferProcessStore.nextForState(eq(DEPROVISIONING.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        when(transferProcessStore.find(process.getId())).thenReturn(process);
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(policyArchive, atLeastOnce()).findPolicyForContract(anyString());
        verify(transferProcessStore).update(argThat(p -> p.getState() == ERROR.code()));
    }

    @Test
    void deprovisioned_shouldTransitionToEnded() throws InterruptedException {
        var process = createTransferProcess(DEPROVISIONED);
        when(transferProcessStore.nextForState(eq(DEPROVISIONED.code()), anyInt())).thenReturn(List.of(process)).thenReturn(emptyList());
        var latch = countDownOnUpdateLatch();

        manager.start();

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(transferProcessStore).update(argThat(p -> p.getState() == ENDED.code()));
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcess(inState, new TransferType(), true);
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type, boolean managed) {
        String processId = UUID.randomUUID().toString();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(processId)
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .transferType(type)
                .managedResources(managed)
                .destinationType(DESTINATION_TYPE)
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(TransferProcess.Type.CONSUMER)
                .id("test-process-" + processId)
                .state(inState.code())
                .dataRequest(dataRequest)
                .build();
    }

    private ProvisionedDataDestinationResource provisionedDataDestinationResource() {
        return new TestProvisionedDataDestinationResource("test-resource", PROVISIONED_RESOURCE_ID);
    }

    private CountDownLatch countDownOnUpdateLatch() {
        return countDownOnUpdateLatch(1);
    }

    private CountDownLatch countDownOnUpdateLatch(int count) {
        var latch = new CountDownLatch(count);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(transferProcessStore).update(any());

        return latch;
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
            this.hasToken = true;
        }
    }

    private static class TestToken implements SecretToken {

        @Override
        public long getExpiration() {
            return 0;
        }

        @Override
        public Map<String, ?> flatten() {
            return null;
        }
    }

}
