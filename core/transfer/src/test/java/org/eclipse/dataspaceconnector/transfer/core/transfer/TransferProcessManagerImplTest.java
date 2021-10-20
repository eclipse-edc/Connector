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

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.niceMock;


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
        manager.registerListener(listener);

        assertThat(manager.getListeners()).containsOnly(listener);
    }

    @Test
    void registerListener_processIdExists_shouldAdd() {
        var listener = new TestListener();
        var listener2 = new TestListener();
        manager.registerListener(listener);
        manager.registerListener(listener2);

        assertThat(manager.getListeners()).hasSize(2).containsOnly(listener, listener2);
    }

    @Test
    void registerListener_processIdAndListenerExists_shouldReplace() {
        var listener = new TestListener();
        manager.registerListener(listener);
        manager.registerListener(listener);

        assertThat(manager.getListeners()).hasSize(1).containsOnly(listener);
    }

    @Test
    void unregisterListener() {
        var listener = new TestListener();
        manager.registerListener(listener);

        manager.unregisterListener(listener);

        assertThat(manager.getListeners()).doesNotContain(listener);
    }


    @Test
    void unregisterListener_listenerNotRegistered() {
        var listener = new TestListener();
        manager.registerListener(listener);

        var listener2 = new TestListener();
        manager.unregisterListener(listener2);

        assertThat(manager.getListeners()).doesNotContain(listener2).containsOnly(listener);
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
