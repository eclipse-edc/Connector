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

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.transfer.store.memory.InMemoryTransferProcessStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class TransferProcessManagerImplConsumerTest {

    private static final long TIMEOUT = 5;
    private static final int TRANSFER_MANAGER_BATCHSIZE = 10;
    private TransferProcessManagerImpl transferProcessManager;
    private ProvisionManager provisionManager;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private StatusCheckerRegistry statusCheckerRegistry;
    private ExponentialWaitStrategy waitStrategyMock;

    @BeforeEach
    void setup() {
        provisionManager = mock(ProvisionManager.class);
        DataFlowManager dataFlowManager = mock(DataFlowManager.class);
        dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);
        expect(manifestGenerator.generateConsumerManifest(anyObject(TransferProcess.class))).andReturn(new ResourceManifest()).anyTimes();
        replay(manifestGenerator);

        statusCheckerRegistry = mock(StatusCheckerRegistry.class);

        waitStrategyMock = partialMockBuilder(ExponentialWaitStrategy.class)
                .withConstructor(1000L)
                .addMockedMethod("success").strictMock();


        transferProcessManager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(waitStrategyMock)
                .batchSize(TRANSFER_MANAGER_BATCHSIZE)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .statusCheckerRegistry(statusCheckerRegistry)
                .build();
    }

    @Test
    void run_shouldProvision() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.INITIAL);
        var cdl = new CountDownLatch(1);
        //prepare provision manager
        provisionManager.provision(anyObject(TransferProcess.class));
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(provisionManager);

        //prepare process store
        TransferProcessStore processStoreMock = niceMock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.singletonList(process));
        processStoreMock.update(process);
        expectLastCall().times(1);
        replay(processStoreMock);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(provisionManager);
        verify(processStoreMock);
        assertThat(process.getState()).describedAs("State should be PROVISIONING").isEqualTo(TransferProcessStates.PROVISIONING.code());
    }

    @Test
    @DisplayName("verifySend: check that the process is in REQUESTED state")
    void verifySend() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.PROVISIONED);
        var cdl = new CountDownLatch(1);
        //prepare provision manager
        expect(dispatcherRegistry.send(eq(Void.class), anyObject(), anyObject())).andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(dispatcherRegistry);

        //prepare process store
        TransferProcessStore processStoreMock = niceMock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.singletonList(process));
        processStoreMock.update(process);
        expectLastCall().times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes(); //ignore any subsequent calls
        replay(processStoreMock);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(dispatcherRegistry);
        verify(processStoreMock);
        assertThat(process.getState()).describedAs("State should be REQUESTED").isEqualTo(TransferProcessStates.REQUESTED.code());
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to finite processes")
    void verifyCheckProvisioned_allAreFinite() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes(); //ignore any subsequent calls
        replay(processStoreMock);


        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(TransferProcessStates.IN_PROGRESS.code());
    }

    @Test
    @DisplayName("checkProvisioned: all resources belong to non-finite processes")
    void verifyCheckProvisioned_allAreNonFinite() throws InterruptedException {
        //arrange
        TransferType type = TransferType.Builder.transferType()
                .isFinite(false).build();

        TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK, type, true);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes(); //ignore any subsequent calls
        replay(processStoreMock);


        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        assertThat(process.getState()).describedAs("State should be STREAMING").isEqualTo(TransferProcessStates.STREAMING.code());
    }

    @Test
    @DisplayName("checkComplete: should transition process with managed resources if checker returns completed")
    void verifyCompletedManagedResources() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyString())).andReturn((i, l) -> true).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
    }

    @Test
    @DisplayName("checkComplete: should transition process with no managed resources if checker returns completed")
    void verifyCompletedNonManagedResources() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK,new TransferType(), false);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyString())).andReturn((i, l) -> true).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
    }

    @Test
    @DisplayName("checkComplete: should not transition process if checker returns not yet completed")
    void verifyCompleted_notAllYetCompleted() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));
        // flip the latch on the next cycle
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andAnswer(() -> {
            cdl.countDown();
            return Collections.emptyList();
        }).anyTimes();
        processStoreMock.update(eq(process));
        expectLastCall().anyTimes();
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyString())).andReturn((i, l) -> false).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(TransferProcessStates.IN_PROGRESS.code());
    }

    @Test
    @DisplayName("checkComplete: should not transition process with managed resources but no status checker")
    void verifyCompleted_noCheckerForManaged() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyString())).andReturn(null).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be IN_PROGRESS").isEqualTo(TransferProcessStates.IN_PROGRESS.code());
    }

    @Test
    @DisplayName("checkComplete: should automatically transition process with no managed resources if no checker")
    void verifyCompleted_noCheckerForSomeResources() throws InterruptedException {
        //arrange
        TransferProcess process = createTransferProcess(TransferProcessStates.IN_PROGRESS, new TransferType(), false);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyString())).andReturn(null).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
    }

    @Test
    @DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve() throws InterruptedException {
        var numProcesses = TRANSFER_MANAGER_BATCHSIZE * 2;

        //prepare process store
        TransferProcessStore inMemoryProcessStore = new InMemoryTransferProcessStore();

        //create a few processes
        var processes = new ArrayList<TransferProcess>();
        for (int i = 0; i < numProcesses; i++) {
            TransferProcess process = createTransferProcess(TransferProcessStates.UNSAVED);
            processes.add(process);
            inMemoryProcessStore.create(process);
        }

        var processesToProvision = new CountDownLatch(numProcesses); //all processes should be provisioned

        //prepare provision manager
        provisionManager.provision(anyObject(TransferProcess.class));
        expectLastCall().andAnswer(() -> {
            processesToProvision.countDown();
            return null;
        }).anyTimes();
        replay(provisionManager);

        // use the waitstrategy to count the number of iterations by making sure "success" was called exactly twice
        waitStrategyMock.success();
        expectLastCall().times(2);
        replay(waitStrategyMock);


        //act
        transferProcessManager.start(inMemoryProcessStore);

        //assert
        assertThat(processesToProvision.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(provisionManager);
        assertThat(processes).describedAs("All transfer processes should be in PROVISIONING state").allSatisfy(p -> {
            var id = p.getId();
            var storedProcess = inMemoryProcessStore.find(id);
            assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore").isNotNull();
            assertThat(storedProcess.getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
        });
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcess(inState, new TransferType(), true);
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type, boolean managed) {

        String processId = UUID.randomUUID().toString();

        DataRequest mock = niceMock(DataRequest.class);
        expect(mock.getTransferType()).andReturn(type).anyTimes();
        expect(mock.isManagedResources()).andReturn(managed).anyTimes();
        expect(mock.getId()).andReturn(processId).anyTimes();
        replay(mock);
        return TransferProcess.Builder.newInstance()
                .state(inState.code())
                .id("test-process-" + processId)
                .provisionedResourceSet(new ProvisionedResourceSet())
                .type(TransferProcess.Type.CONSUMER)
                .dataRequest(mock)
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
