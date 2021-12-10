package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.easymock.Capture;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.TransferResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntry;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandler;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

class SyncTransferProcessManagerTest {

    public static final String TEST_TYPE = "test-type";
    private SyncTransferProcessManager syncTransferProcessManager;
    private RemoteMessageDispatcherRegistry messageDispatcherRegistry;
    private TransferProcessStore transferProcessStore;
    private DataProxyManager dataProxyManager;

    @BeforeEach
    void setUp() {

        dataProxyManager = niceMock(DataProxyManager.class);
        transferProcessStore = mock(TransferProcessStore.class);
        messageDispatcherRegistry = niceMock(RemoteMessageDispatcherRegistry.class);
        ProxyEntryHandler proxyEntryHandlerMock = niceMock(ProxyEntryHandler.class);
        Map<String, ProxyEntryHandler> handlers = Map.of(TEST_TYPE, proxyEntryHandlerMock);
        syncTransferProcessManager = new SyncTransferProcessManager(dataProxyManager, transferProcessStore, messageDispatcherRegistry, handlers, new TypeManager());
    }

    @Test
    void initiateConsumerRequest() {
        Capture<TransferProcess> tpCapture = newCapture();
        transferProcessStore.create(capture(tpCapture));
        expectLastCall();
        expect(transferProcessStore.find(anyString())).andAnswer(tpCapture::getValue); //short-wire it

        expect(messageDispatcherRegistry.send(anyObject(), anyObject(), anyObject())).andReturn(CompletableFuture.completedFuture(createProxyEntry()));
        replay(transferProcessStore, messageDispatcherRegistry);

        DataRequest request = createRequest();
        var result = syncTransferProcessManager.initiateConsumerRequest(request);
        assertSuccess(result);
        assertThat(tpCapture.getValue().getState()).isEqualTo(TransferProcessStates.COMPLETED.code());
        assertThat(tpCapture.getValue().getErrorDetail()).isNull();

        verify(transferProcessStore, messageDispatcherRegistry);
    }

    @Test
    void initiateConsumerRequest_dispatcherReturnsNull() {
        Capture<TransferProcess> tpCapture = newCapture();
        transferProcessStore.create(capture(tpCapture));
        expectLastCall();
        replay(transferProcessStore);
        DataRequest request = createRequest();
        var result = syncTransferProcessManager.initiateConsumerRequest(request);
        assertThat(result.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);

        verify(transferProcessStore);

    }

    @Test
    void initiateConsumerRequest_transferProcessError() {
        Capture<TransferProcess> tpCapture = newCapture();
        transferProcessStore.create(capture(tpCapture));
        expectLastCall();
        expect(transferProcessStore.find(anyString())).andAnswer(() -> {
            var tp = tpCapture.getValue();
            tp.transitionError("test error");
            return tp;
        }); //short-wire it
        expect(messageDispatcherRegistry.send(anyObject(), anyObject(), anyObject())).andReturn(CompletableFuture.completedFuture(createProxyEntry()));
        replay(transferProcessStore, messageDispatcherRegistry);

        DataRequest request = createRequest();
        var result = syncTransferProcessManager.initiateConsumerRequest(request);
        assertThat(result.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getError()).isEqualTo("test error");
        verify(transferProcessStore, messageDispatcherRegistry);

    }

    @Test
    void initiateProviderRequest_noProxyFound() {
        var request = createRequest();
        Capture<TransferProcess> tpCapture = newCapture();
        transferProcessStore.create(capture(tpCapture));
        expectLastCall();
        expect(dataProxyManager.getProxy(request)).andReturn(null);
        replay(transferProcessStore, messageDispatcherRegistry, dataProxyManager);

        var result = syncTransferProcessManager.initiateProviderRequest(request);

        assertThat(result.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);
        verify(transferProcessStore, messageDispatcherRegistry, dataProxyManager);
    }

    @Test
    void initiateProviderRequest() {
        var request = createRequest();
        Capture<TransferProcess> tpCapture = newCapture();
        transferProcessStore.create(capture(tpCapture));
        expectLastCall();
        expect(dataProxyManager.getProxy(request)).andReturn(rq -> createProxyEntry());
        replay(transferProcessStore, messageDispatcherRegistry, dataProxyManager);

        var result = syncTransferProcessManager.initiateProviderRequest(request);
        assertSuccess(result);
        assertThat(result.getData()).isInstanceOf(ProxyEntry.class).extracting("type").isEqualTo(TEST_TYPE);

    }

    private DataRequest createRequest() {
        return DataRequest.Builder.newInstance()
                .destinationType(TEST_TYPE)
                .isSync(true)
                .build();
    }

    private void assertSuccess(TransferResponse result) {
        assertThat(result.getStatus()).isEqualTo(ResponseStatus.OK);
    }

    @NotNull
    private ProxyEntry createProxyEntry() {
        return ProxyEntry.Builder.newInstance().type(TEST_TYPE).build();
    }

}