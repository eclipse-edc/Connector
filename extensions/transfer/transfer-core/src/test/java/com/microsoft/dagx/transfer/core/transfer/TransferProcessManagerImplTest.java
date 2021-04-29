package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.easymock.EasyMock.createNiceMock;

/**
 *
 */
class TransferProcessManagerImplTest {


    /**
     * All creations operations must be idempotent in order to support reliability (e.g. messages/requests may be delivered more than once).
     */
    @Test
    void verifyIdempotency() {
        TransferProcessStore store = EasyMock.createMock(TransferProcessStore.class);

        EasyMock.expect(store.processIdForTransferId("1")).andReturn(null);  // first invoke returns no as there is no store process

        store.create(EasyMock.isA(TransferProcess.class)); // store should only be called once
        EasyMock.expectLastCall();

        EasyMock.expect(store.processIdForTransferId("1")).andReturn("2");

        EasyMock.expect(store.nextForState(EasyMock.anyInt(), EasyMock.anyInt())).andReturn(Collections.emptyList()).anyTimes();

        EasyMock.replay(store);

        TransferProcessManagerImpl manager = TransferProcessManagerImpl.Builder.newInstance()
                .dispatcherRegistry(createNiceMock(RemoteMessageDispatcherRegistry.class))
                .provisionManager(createNiceMock(ProvisionManager.class))
                .dataFlowManager(createNiceMock(DataFlowManager.class))
                .monitor(createNiceMock(Monitor.class))
                .manifestGenerator(createNiceMock(ResourceManifestGenerator.class)).build();

        manager.start(store);

        DataRequest dataRequest = DataRequest.Builder.newInstance().id("1").destinationType("test").build();

        manager.initiateProviderRequest(dataRequest);

        // repeat request
        manager.initiateProviderRequest(dataRequest);

        manager.stop();

        EasyMock.verify(store); // verify the process was only stored once
    }
}
