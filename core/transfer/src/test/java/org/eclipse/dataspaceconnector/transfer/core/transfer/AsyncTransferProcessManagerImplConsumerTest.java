/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.core.base.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.core.TestResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED_ACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncTransferProcessManagerImplConsumerTest {

    private static final String TEST_TYPE = "test-type";
    private static final long TIMEOUT = 5;
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private final ProvisionManager provisionManager = mock(ProvisionManager.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final StatusCheckerRegistry statusCheckerRegistry = mock(StatusCheckerRegistry.class);
    private final ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final DataProxyManager dataProxyManager = mock(DataProxyManager.class);
    private DefaultTransferProcessManager transferProcessManager;

    @BeforeEach
    void setup() {
        var resourceManifest = ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        when(manifestGenerator.generateConsumerManifest(any(TransferProcess.class))).thenReturn(resourceManifest);

        DefaultProxyEntryHandlerRegistry proxyEntryHandlerRegistry = new DefaultProxyEntryHandlerRegistry();
        proxyEntryHandlerRegistry.put(TEST_TYPE, mock(ProxyEntryHandler.class));

        transferProcessManager = DefaultTransferProcessManager.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(mock(DataFlowManager.class))
                .waitStrategy(mock(ExponentialWaitStrategy.class))
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .commandQueue(mock(CommandQueue.class))
                .commandRunner(mock(CommandRunner.class))
                .typeManager(new TypeManager())
                .statusCheckerRegistry(statusCheckerRegistry)
                .dataProxyManager(dataProxyManager)
                .proxyEntryHandlerRegistry(proxyEntryHandlerRegistry)
                .build();
    }

    @Test
    void run_shouldProvision() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(INITIAL);
        var cdl = new CountDownLatch(1);
        //prepare provision manager
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(provisionManager).provision(any(TransferProcess.class));

        when(store.nextForState(eq(INITIAL.code()), anyInt())).thenReturn(List.of(process));

        store.update(process);
        doNothing().when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be PROVISIONING").isEqualTo(TransferProcessStates.PROVISIONING.code());
        verify(provisionManager, atLeastOnce()).provision(any(TransferProcess.class));
        verify(store, atLeastOnce()).nextForState(eq(INITIAL.code()), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("verifySend: check that the process is in REQUESTED state")
    void verifySend() throws InterruptedException {
        TransferProcess process = createTransferProcess(PROVISIONED);
        var cdl = new CountDownLatch(1);

        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(dispatcherRegistry).send(eq(Object.class), any(), any());

        when(store.nextForState(eq(PROVISIONED.code()), anyInt())).thenReturn(List.of(process));
        doNothing().when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be REQUESTED").isEqualTo(TransferProcessStates.REQUESTED.code());
        verify(dispatcherRegistry, atLeastOnce()).send(any(), any(), any());
        verify(store, atLeastOnce()).nextForState(eq(INITIAL.code()), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to finite processes")
    void verifyCheckProvisioned_allAreFinite() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(REQUESTED_ACK.code()), anyInt())).thenReturn(List.of(process));

        store.update(process);
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(IN_PROGRESS.code());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to non-finite processes")
    void verifyCheckProvisioned_allAreNonFinite() throws InterruptedException {
        TransferType type = TransferType.Builder.transferType()
                .isFinite(false).build();

        TransferProcess process = createTransferProcess(REQUESTED_ACK, type, true);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(REQUESTED_ACK.code()), anyInt())).thenReturn(List.of(process));

        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be STREAMING").isEqualTo(TransferProcessStates.STREAMING.code());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkProvisioned: empty provisioned resources")
    void verifyCheckProvisioned_emptyProvisionedResoures() throws InterruptedException {
        TransferProcess process = createTransferProcess(REQUESTED_ACK);

        var cdl = new CountDownLatch(1);

        //prepare process store
        when(store.nextForState(eq(REQUESTED_ACK.code()), anyInt())).thenReturn(List.of(process));

        // flip the latch on the next cycle
        when(store.nextForState(anyInt(), anyInt())).thenAnswer(i -> {
            cdl.countDown();
            return emptyList();
        });

        store.update(process);
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be REQUESTED_ACK").isEqualTo(REQUESTED_ACK.code());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkComplete: should transition process with managed resources if checker returns completed")
    void verifyCompletedManagedResources() throws InterruptedException {
        TransferProcess process = createTransferProcess(REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(IN_PROGRESS.code()), anyInt()))
                .thenReturn(List.of(process)).thenReturn(emptyList());
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(store).update(any());

        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkComplete: should transition process with no managed resources if checker returns completed")
    void verifyCompletedNonManagedResources() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(REQUESTED_ACK, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process));

        store.update(process);
        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(store).update(process);

        when(statusCheckerRegistry.resolve(anyString())).thenReturn((i, l) -> true);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("checkComplete: should not transition process if checker returns not yet completed")
    void verifyCompleted_notAllYetCompleted() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process));
        when(store.nextForState(anyInt(), anyInt())).thenAnswer(i -> {
            cdl.countDown();
            return emptyList();
        });
        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(IN_PROGRESS.code());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
    }

    @Test
    @DisplayName("checkComplete: should not transition process with managed resources but no status checker")
    void verifyCompleted_noCheckerForManaged() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        when(store.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process));
        // flip the latch on the next cycle
        when(store.nextForState(anyInt(), anyInt())).thenAnswer(i -> {
            cdl.countDown();
            return emptyList();
        });

        doThrow(new AssertionError("update() should not be called as process was not updated"))
                .when(store).update(process);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(IN_PROGRESS.code());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
    }

    @Test
    @DisplayName("checkComplete: should automatically transition process with no managed resources if no checker")
    void verifyCompleted_noCheckerForSomeResources() throws InterruptedException {
        TransferProcess process = createTransferProcess(IN_PROGRESS, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        when(store.nextForState(eq(IN_PROGRESS.code()), anyInt())).thenReturn(List.of(process));

        doAnswer(i -> {
            cdl.countDown();
            return null;
        }).when(store).update(process);

        when(statusCheckerRegistry.resolve(anyString())).thenReturn(null);

        transferProcessManager.start(store);

        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
        verify(statusCheckerRegistry, atLeastOnce()).resolve(any());
        verify(store, atLeastOnce()).nextForState(anyInt(), anyInt());
        verify(store, atLeastOnce()).update(process);
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() throws InterruptedException {
        var numProcesses = TRANSFER_MANAGER_BATCHSIZE * 2;

        TransferProcessStore inMemoryProcessStore = new InMemoryTransferProcessStore();

        var processes = new ArrayList<TransferProcess>();
        for (int i = 0; i < numProcesses; i++) {
            ProvisionedResourceSet resourceSet = ProvisionedResourceSet.Builder.newInstance().resources(List.of(new TestResource())).build();
            TransferProcess process = createTransferProcess(TransferProcessStates.UNSAVED).toBuilder().provisionedResourceSet(resourceSet).build();
            process.transitionInitial();
            processes.add(process);
            inMemoryProcessStore.create(process);
        }

        var processesToProvision = new CountDownLatch(numProcesses); //all processes should be provisioned

        doAnswer(i -> {
            processesToProvision.countDown();
            return null;
        }).when(provisionManager).provision(any(TransferProcess.class));

        transferProcessManager.start(inMemoryProcessStore);

        assertThat(processesToProvision.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        assertThat(processes).describedAs("All transfer processes should be in PROVISIONING state").allSatisfy(p -> {
            var id = p.getId();
            var storedProcess = inMemoryProcessStore.find(id);
            assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore").isNotNull();
            assertThat(storedProcess.getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
        });
        verify(provisionManager, atLeastOnce()).provision(any());
    }

    @Test
    void sync_consumer_request_should_create_a_transfer_process_already_completed() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(store).create(tpCapture.capture());
        when(store.find(anyString())).thenAnswer(i -> tpCapture.getValue()); //short-wire it
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(createObjectWithPayload()));
        DataRequest request = createSyncRequest();
        transferProcessManager.start(store);

        var result = transferProcessManager.initiateConsumerRequest(request);

        assertSuccess(result);
        assertThat(tpCapture.getValue().getState()).isEqualTo(TransferProcessStates.COMPLETED.code());
        assertThat(tpCapture.getValue().getErrorDetail()).isNull();
        verify(store).find(anyString());
        verify(dispatcherRegistry).send(any(), any(), any());
    }

    @Test
    void sync_consumer_request_should_return_error_if_dispatcher_fails() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(store).create(tpCapture.capture());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        DataRequest request = createSyncRequest();
        transferProcessManager.start(store);

        var result = transferProcessManager.initiateConsumerRequest(request);

        assertThat(result.getFailure()).isNotNull().extracting(ResponseFailure::status)
                .isEqualTo(ResponseStatus.FATAL_ERROR);
        verify(store).create(tpCapture.capture());
    }

    @Test
    void sync_consumer_request_should_return_error_if_transfer_process_went_to_error_state() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(store).create(tpCapture.capture());
        when(store.find(anyString())).thenAnswer(i -> {
            var tp = tpCapture.getValue();
            tp.transitionError("test error");
            return tp;
        }); //short-wire it
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(createObjectWithPayload()));
        DataRequest request = createSyncRequest();
        transferProcessManager.start(store);

        var result = transferProcessManager.initiateConsumerRequest(request);

        assertThat(result.getFailure())
                .isNotNull()
                .satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(ResponseStatus.FATAL_ERROR);
                    assertThat(failure.getMessages()).containsExactly("test error");
                });
        verify(store).create(tpCapture.capture());
        verify(store).find(anyString());
        verify(dispatcherRegistry).send(any(), any(), any());
    }

    @Test
    void sync_provider_request_should_create_a_transfer_process_already_completed() {
        var request = createSyncRequest();
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(store).create(tpCapture.capture());
        when(dataProxyManager.getProxy(request)).thenReturn(rq -> createProxyEntry());
        transferProcessManager.start(store);

        var result = transferProcessManager.initiateProviderRequest(request);

        assertSuccess(result);
        assertThat(result.getData()).isInstanceOf(ProxyEntry.class).extracting("type").isEqualTo(TEST_TYPE);
        verify(store).create(tpCapture.capture());
        verify(dataProxyManager).getProxy(request);
    }

    @Test
    void sync_provider_request_should_return_error_if_proxy_does_not_exist() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(store).create(tpCapture.capture());
        var request = createSyncRequest();
        when(dataProxyManager.getProxy(request)).thenReturn(null);
        transferProcessManager.start(store);

        var result = transferProcessManager.initiateProviderRequest(request);

        assertThat(result.getFailure())
                .isNotNull()
                .extracting(ResponseFailure::status)
                .isEqualTo(ResponseStatus.FATAL_ERROR);
        verify(store).create(tpCapture.capture());
        verify(dataProxyManager).getProxy(request);
    }

    private void assertSuccess(TransferInitiateResult result) {
        assertThat(result.succeeded()).isTrue();
    }

    private DataRequest createSyncRequest() {
        return DataRequest.Builder.newInstance()
                .id("test-datarequest-id")
                .destinationType(TEST_TYPE)
                .isSync(true)
                .build();
    }

    //creates an anonymous object that has a String field named "payload", which is
    //what the Sync TPM expects
    private Object createObjectWithPayload() {
        try {
            return new Object() {
                private final String payload = new ObjectMapper().writeValueAsString(createProxyEntry());
            };
        } catch (JsonProcessingException ex) {
            throw new EdcException(ex);
        }
    }

    @NotNull
    private ProxyEntry createProxyEntry() {
        return ProxyEntry.Builder.newInstance().type(TEST_TYPE).build();
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcess(inState, new TransferType(), true);
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type, boolean managed) {
        String processId = UUID.randomUUID().toString();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(processId)
                .transferType(type)
                .managedResources(managed)
                .dataDestination(DataAddress.Builder.newInstance().type("destination-type").build())
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(TransferProcess.Type.CONSUMER)
                .id("test-process-" + processId)
                .state(inState.code())
                .dataRequest(dataRequest)
                .build();
    }

    private static class TestResource extends ProvisionedDataDestinationResource {
        protected TestResource() {
            super();
        }

        @Override
        public DataAddress createDataDestination() {
            return null;
        }

        @Override
        public String getResourceName() {
            return "test-resource";
        }
    }
}
