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
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.BeforeEach;
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
    private DataFlowManager dataFlowManager;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private ResourceManifestGenerator manifestGenerator;
    private StatusCheckerRegistry statusCheckerRegistry;

    @BeforeEach
    void setup() {
        provisionManager = mock(ProvisionManager.class);
        dataFlowManager = mock(DataFlowManager.class);
        dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        manifestGenerator = mock(ResourceManifestGenerator.class);

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
    void verifySend_whenClient() throws InterruptedException {
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


    private TransferProcess createTransferProcess(TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id("test-process-id")
                .type(TransferProcess.Type.CLIENT)
                .dataRequest(mock(DataRequest.class))
                .build();
    }
}
