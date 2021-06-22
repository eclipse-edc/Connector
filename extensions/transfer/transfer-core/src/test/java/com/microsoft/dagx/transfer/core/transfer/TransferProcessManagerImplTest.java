/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferProcessListener;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.niceMock;

/**
 *
 */
class TransferProcessManagerImplTest {


    private TransferProcessManagerImpl manager;
    private TransferProcessStore store;


    @BeforeEach
    void setup() {
        store = EasyMock.createMock(TransferProcessStore.class);

        EasyMock.expect(store.processIdForTransferId("1")).andReturn(null);  // first invoke returns no as there is no store process

        store.create(EasyMock.isA(TransferProcess.class)); // store should only be called once
        EasyMock.expectLastCall();

        EasyMock.expect(store.processIdForTransferId("1")).andReturn("2");

        EasyMock.expect(store.nextForState(EasyMock.anyInt(), EasyMock.anyInt())).andReturn(Collections.emptyList()).anyTimes();

        EasyMock.replay(store);

        manager = TransferProcessManagerImpl.Builder.newInstance()
                .dispatcherRegistry(createNiceMock(RemoteMessageDispatcherRegistry.class))
                .provisionManager(createNiceMock(ProvisionManager.class))
                .dataFlowManager(createNiceMock(DataFlowManager.class))
                .monitor(createNiceMock(Monitor.class))
                .statusCheckerRegistry(niceMock(StatusCheckerRegistry.class))
                .manifestGenerator(createNiceMock(ResourceManifestGenerator.class)).build();
        manager.start(store);
    }

    /**
     * All creations operations must be idempotent in order to support reliability (e.g. messages/requests may be delivered more than once).
     */
    @Test
    void verifyIdempotency() {

        DataRequest dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();
        manager.initiateProviderRequest(dataRequest);

        // repeat request
        manager.initiateProviderRequest(dataRequest);
        manager.stop();
        EasyMock.verify(store); // verify the process was only stored once
    }

    @Test
    void registerListener() {
        var listener = new TestListener();
        manager.registerListener("test-process", listener);

        assertThat(manager.getListeners()).containsKey("test-process");
        assertThat(manager.getListeners().get("test-process")).containsOnly(listener);
    }

    @Test
    void registerListener_processIdExists_shouldAdd() {
        var listener = new TestListener();
        var listener2 = new TestListener();
        manager.registerListener("test-process", listener);
        manager.registerListener("test-process", listener2);

        assertThat(manager.getListeners()).containsKey("test-process");
        assertThat(manager.getListeners().get("test-process")).hasSize(2).containsOnly(listener, listener2);
    }

    @Test
    void registerListener_processIdAndListenerExists_shouldReplace() {
        var listener = new TestListener();
        manager.registerListener("test-process", listener);
        manager.registerListener("test-process", listener);

        assertThat(manager.getListeners()).containsKey("test-process");
        assertThat(manager.getListeners().get("test-process")).hasSize(1).containsOnly(listener);
    }

    @Test
    void unregisterListener() {
        var listener = new TestListener();
        var pid = "test-pid";
        manager.registerListener(pid, listener);

        manager.unregister(listener);

        assertThat(manager.getListeners()).doesNotContainKey(pid);
    }


    @Test
    void unregisterListener_listenerNotRegistered() {
        var listener = new TestListener();
        var pid = "test-pid";
        manager.registerListener(pid, listener);

        var listener2 = new TestListener();
        manager.unregister(listener2);

        assertThat(manager.getListeners()).containsKey(pid);
        assertThat(manager.getListeners().get(pid)).doesNotContain(listener2).containsOnly(listener);
    }

    private static class TestListener implements TransferProcessListener {
        @Override
        public void completed(TransferProcess process) {

        }

        @Override
        public void deprovisioned(TransferProcess process) {

        }
    }
}
