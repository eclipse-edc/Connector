/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.*;

public class TransferManagerImplConsumerTest {

    private static final long TIMEOUT = 5;
    private TransferProcessManagerImpl transferProcessManager;
    private ProvisionManager provisionManager;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private StatusCheckerRegistry statusCheckerRegistry;

    @BeforeEach
    void setup() {
        provisionManager = mock(ProvisionManager.class);
        DataFlowManager dataFlowManager = mock(DataFlowManager.class);
        dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        ResourceManifestGenerator manifestGenerator = mock(ResourceManifestGenerator.class);

        statusCheckerRegistry = mock(StatusCheckerRegistry.class);
        transferProcessManager = TransferProcessManagerImpl.Builder.newInstance()
                .provisionManager(provisionManager)
                .dataFlowManager(dataFlowManager)
                .waitStrategy(new ExponentialWaitStrategy(1000))
                .batchSize(10)
                .dispatcherRegistry(dispatcherRegistry)
                .manifestGenerator(manifestGenerator)
                .monitor(mock(Monitor.class))
                .statusCheckerRegistry(statusCheckerRegistry)
                .build();
    }

    @Test
    void run_shouldProvision() throws InterruptedException {
        //arrange
        final TransferProcess process = createTransferProcess(TransferProcessStates.INITIAL);
        var cdl = new CountDownLatch(1);
        //prepare provision manager
        provisionManager.provision(anyObject(TransferProcess.class));
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(provisionManager);

        //prepare process store
        final TransferProcessStore processStoreMock = niceMock(TransferProcessStore.class);
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
        final TransferProcess process = createTransferProcess(TransferProcessStates.PROVISIONED);
        var cdl = new CountDownLatch(1);
        //prepare provision manager
        expect(dispatcherRegistry.send(eq(Void.class), anyObject(), anyObject())).andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        replay(dispatcherRegistry);

        //prepare process store
        final TransferProcessStore processStoreMock = niceMock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.singletonList(process));
        processStoreMock.update(process);
        expectLastCall().times(1);
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
        final TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        final TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();//ignore any subsequent calls
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
        final TransferType type = TransferType.Builder.transferType()
                .isFinite(false).build();

        final TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK, type);
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        final TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.singletonList(process));

        processStoreMock.update(process);
        expectLastCall().andAnswer(() -> {
            cdl.countDown();
            return null;
        }).times(1);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andReturn(Collections.emptyList()).anyTimes();//ignore any subsequent calls
        replay(processStoreMock);


        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        assertThat(process.getState()).describedAs("State should be STREAMING").isEqualTo(TransferProcessStates.STREAMING.code());
    }

    @Test
    @DisplayName("checkComplete: all ProvisionedResources are complete")
    void verifyCompleted_allCompleted() throws InterruptedException {
        //arrange
        final TransferProcess process = createTransferProcess(TransferProcessStates.REQUESTED_ACK);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        final TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
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
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(pr -> true).times(4);
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
    @DisplayName("checkComplete: not all ProvisionedResources are yet completed")
    void verifyComnpleted_notAllYetCompleted() throws InterruptedException {
        //arrange
        final TransferProcess process = createTransferProcess(TransferProcessStates.IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        final TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
        expect(processStoreMock.nextForState(eq(TransferProcessStates.INITIAL.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.PROVISIONED.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.REQUESTED_ACK.code()), anyInt())).andReturn(Collections.emptyList());
        expect(processStoreMock.nextForState(eq(TransferProcessStates.IN_PROGRESS.code()), anyInt())).andReturn(Collections.singletonList(process));
        // flip the latch on the next cycle
        expect(processStoreMock.nextForState(anyInt(), anyInt())).andAnswer(() -> {
            cdl.countDown();
            return Collections.emptyList();
        }).anyTimes();
        replay(processStoreMock);

        // prepare statuschecker registry
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(pr -> true).times(3);
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(pr -> false).times(1);
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
    @DisplayName("checkComplete: Should ignore resources without StatusCheckers")
    void verifyCompleted_noCheckerForSomeResources() throws InterruptedException {
        //arrange
        final TransferProcess process = createTransferProcess(TransferProcessStates.IN_PROGRESS);
        process.getProvisionedResourceSet().addResource(new TestResource());
        process.getProvisionedResourceSet().addResource(new TestResource());

        var cdl = new CountDownLatch(1);

        //prepare process store
        final TransferProcessStore processStoreMock = mock(TransferProcessStore.class);
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
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(pr -> true).times(1);
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(null).times(1);
        expect(statusCheckerRegistry.resolve(anyObject(TestResource.class))).andReturn(pr -> true).times(1);
        replay(statusCheckerRegistry);

        //act
        transferProcessManager.start(processStoreMock);

        //assert
        assertThat(cdl.await(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        verify(processStoreMock);
        verify(statusCheckerRegistry);
        assertThat(process.getState()).describedAs("State should be COMPLETED").isEqualTo(TransferProcessStates.COMPLETED.code());
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState) {
        return createTransferProcess(inState, new TransferType());
    }

    private TransferProcess createTransferProcess(TransferProcessStates inState, TransferType type) {
        final DataRequest mock = niceMock(DataRequest.class);
        expect(mock.getTransferType()).andReturn(type).anyTimes();
        replay(mock);
        return TransferProcess.Builder.newInstance()
                .state(inState.code())
                .id("test-process-id")
                .provisionedResourceSet(new ProvisionedResourceSet())
                .type(TransferProcess.Type.CLIENT)
                .dataRequest(mock)
                .build();
    }

    private static class TestResource extends ProvisionedResource {
    }
}
