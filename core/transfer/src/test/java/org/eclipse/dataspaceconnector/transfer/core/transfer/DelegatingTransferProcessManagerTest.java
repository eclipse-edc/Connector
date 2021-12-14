package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class DelegatingTransferProcessManagerTest {
    private DelegatingTransferProcessManager manager;
    private AsyncTransferProcessManager asyncDelegate;
    private SyncTransferProcessManager syncDelegate;

    @BeforeEach
    void setup() {
        asyncDelegate = strictMock(AsyncTransferProcessManager.class);
        syncDelegate = strictMock(SyncTransferProcessManager.class);

        manager = new DelegatingTransferProcessManager(asyncDelegate, syncDelegate);
    }

    @Test
    void initiateConsumerRequest_sync() {
        var request = createRequest(true);
        expect(syncDelegate.initiateConsumerRequest(request)).andReturn(TransferInitiateResult.success(""));
        replay(syncDelegate, asyncDelegate);

        var result = manager.initiateConsumerRequest(request);

        assertThat(result.succeeded()).isTrue();
        verify(syncDelegate, asyncDelegate);
    }

    @Test
    void initiateConsumerRequest_async() {
        var request = createRequest(false);
        expect(asyncDelegate.initiateConsumerRequest(request)).andReturn(TransferInitiateResult.success(""));
        replay(syncDelegate, asyncDelegate);

        var result = manager.initiateConsumerRequest(request);

        assertThat(result.succeeded()).isTrue();
        verify(syncDelegate, asyncDelegate);
    }

    @Test
    void initiateProviderRequest_sync() {
        var request = createRequest(true);
        expect(syncDelegate.initiateProviderRequest(request)).andReturn(TransferInitiateResult.success(""));
        replay(syncDelegate, asyncDelegate);

        var result = manager.initiateProviderRequest(request);

        assertThat(result.succeeded()).isTrue();
        verify(syncDelegate, asyncDelegate);
    }

    @Test
    void initiateProviderRequest_async() {
        var request = createRequest(false);
        expect(asyncDelegate.initiateProviderRequest(request)).andReturn(TransferInitiateResult.success(""));
        replay(syncDelegate, asyncDelegate);

        var result = manager.initiateProviderRequest(request);

        assertThat(result.succeeded()).isTrue();
        verify(syncDelegate, asyncDelegate);
    }

    @Test
    void start() {
        asyncDelegate.start(anyObject());
        expectLastCall();
        replay(syncDelegate, asyncDelegate);

        manager.start(niceMock(TransferProcessStore.class));

        verify(syncDelegate, asyncDelegate);
    }

    @Test
    void stop() {
        asyncDelegate.stop();
        expectLastCall();
        replay(syncDelegate, asyncDelegate);

        manager.stop();

        verify(syncDelegate, asyncDelegate);
    }


    private DataRequest createRequest(boolean isSync) {
        return DataRequest.Builder.newInstance()
                .destinationType("test-type")
                .isSync(isSync)
                .build();
    }
}