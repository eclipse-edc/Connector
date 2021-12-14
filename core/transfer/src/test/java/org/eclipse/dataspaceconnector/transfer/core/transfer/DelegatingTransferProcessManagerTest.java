package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelegatingTransferProcessManagerTest {
    private DelegatingTransferProcessManager manager;
    private AsyncTransferProcessManager asyncDelegate;
    private SyncTransferProcessManager syncDelegate;

    @BeforeEach
    void setup() {
        asyncDelegate = mock(AsyncTransferProcessManager.class);
        syncDelegate = mock(SyncTransferProcessManager.class);

        manager = new DelegatingTransferProcessManager(asyncDelegate, syncDelegate);
    }

    @Test
    void initiateConsumerRequest_sync() {
        var request = createRequest(true);
        when(syncDelegate.initiateConsumerRequest(request)).thenReturn(TransferInitiateResult.success(""));

        var result = manager.initiateConsumerRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void initiateConsumerRequest_async() {
        var request = createRequest(false);
        when(asyncDelegate.initiateConsumerRequest(request)).thenReturn(TransferInitiateResult.success(""));

        var result = manager.initiateConsumerRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void initiateProviderRequest_sync() {
        var request = createRequest(true);
        when(syncDelegate.initiateProviderRequest(request)).thenReturn(TransferInitiateResult.success(""));

        var result = manager.initiateProviderRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void initiateProviderRequest_async() {
        var request = createRequest(false);
        when(asyncDelegate.initiateProviderRequest(request)).thenReturn(TransferInitiateResult.success(""));

        var result = manager.initiateProviderRequest(request);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void start() {
        doNothing().when(asyncDelegate).start(any());
        TransferProcessStore store = mock(TransferProcessStore.class);

        manager.start(store);

        verify(asyncDelegate).start(store);
    }

    @Test
    void stop() {
        manager.stop();

        verify(asyncDelegate).stop();
    }


    private DataRequest createRequest(boolean isSync) {
        return DataRequest.Builder.newInstance()
                .destinationType("test-type")
                .isSync(isSync)
                .build();
    }
}