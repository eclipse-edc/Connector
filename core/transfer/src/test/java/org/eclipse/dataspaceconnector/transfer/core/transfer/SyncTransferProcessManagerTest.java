package org.eclipse.dataspaceconnector.transfer.core.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.response.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
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
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncTransferProcessManagerTest {

    public static final String TEST_TYPE = "test-type";
    private SyncTransferProcessManager syncTransferProcessManager;
    private RemoteMessageDispatcherRegistry messageDispatcherRegistry;
    private TransferProcessStore transferProcessStore;
    private DataProxyManager dataProxyManager;

    @BeforeEach
    void setUp() {

        dataProxyManager = mock(DataProxyManager.class);
        transferProcessStore = mock(TransferProcessStore.class);
        messageDispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        ProxyEntryHandler proxyEntryHandlerMock = mock(ProxyEntryHandler.class);
        Map<String, ProxyEntryHandler> handlers = Map.of(TEST_TYPE, proxyEntryHandlerMock);
        syncTransferProcessManager = new SyncTransferProcessManager(dataProxyManager, transferProcessStore, messageDispatcherRegistry, handlers, new TypeManager());
    }

    @Test
    void initiateConsumerRequest() throws JsonProcessingException {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(transferProcessStore).create(tpCapture.capture());
        when(transferProcessStore.find(anyString())).thenAnswer(i -> tpCapture.getValue()); //short-wire it
        when(messageDispatcherRegistry.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(createObjectWithPayload()));
        DataRequest request = createRequest();

        var result = syncTransferProcessManager.initiateConsumerRequest(request);

        assertSuccess(result);
        assertThat(tpCapture.getValue().getState()).isEqualTo(TransferProcessStates.COMPLETED.code());
        assertThat(tpCapture.getValue().getErrorDetail()).isNull();
        verify(transferProcessStore).find(anyString());
        verify(messageDispatcherRegistry).send(any(), any(), any());
    }

    @Test
    void initiateConsumerRequest_dispatcherReturnsNull() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(transferProcessStore).create(tpCapture.capture());
        DataRequest request = createRequest();

        var result = syncTransferProcessManager.initiateConsumerRequest(request);

        assertThat(result.getFailure()).isNotNull().extracting(ResponseFailure::status)
                .isEqualTo(ResponseStatus.FATAL_ERROR);
        verify(transferProcessStore).create(tpCapture.capture());
    }

    @Test
    void initiateConsumerRequest_transferProcessError() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(transferProcessStore).create(tpCapture.capture());
        when(transferProcessStore.find(anyString())).thenAnswer(i -> {
            var tp = tpCapture.getValue();
            tp.transitionError("test error");
            return tp;
        }); //short-wire it
        when(messageDispatcherRegistry.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(createObjectWithPayload()));

        DataRequest request = createRequest();

        var result = syncTransferProcessManager.initiateConsumerRequest(request);

        assertThat(result.getFailure())
                .isNotNull()
                .satisfies(failure -> {
                    assertThat(failure.status()).isEqualTo(ResponseStatus.FATAL_ERROR);
                    assertThat(failure.getMessages()).containsExactly("test error");
                });
        verify(transferProcessStore).create(tpCapture.capture());
        verify(transferProcessStore).find(anyString());
        verify(messageDispatcherRegistry).send(any(), any(), any());
    }

    @Test
    void initiateProviderRequest_noProxyFound() {
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(transferProcessStore).create(tpCapture.capture());
        var request = createRequest();
        when(dataProxyManager.getProxy(request)).thenReturn(null);

        var result = syncTransferProcessManager.initiateProviderRequest(request);

        assertThat(result.getFailure())
                .isNotNull()
                .extracting(ResponseFailure::status)
                .isEqualTo(ResponseStatus.FATAL_ERROR);
        verify(transferProcessStore).create(tpCapture.capture());
        verify(dataProxyManager).getProxy(request);
    }

    @Test
    void initiateProviderRequest() {
        var request = createRequest();
        var tpCapture = ArgumentCaptor.forClass(TransferProcess.class);
        doNothing().when(transferProcessStore).create(tpCapture.capture());
        when(dataProxyManager.getProxy(request)).thenReturn(rq -> createProxyEntry());

        var result = syncTransferProcessManager.initiateProviderRequest(request);

        assertSuccess(result);
        assertThat(result.getData()).isInstanceOf(ProxyEntry.class).extracting("type").isEqualTo(TEST_TYPE);
        verify(transferProcessStore).create(tpCapture.capture());
        verify(dataProxyManager).getProxy(request);
    }

    private DataRequest createRequest() {
        return DataRequest.Builder.newInstance()
                .id("test-datarequest-id")
                .destinationType(TEST_TYPE)
                .isSync(true)
                .build();
    }

    private void assertSuccess(TransferInitiateResult result) {
        assertThat(result.succeeded()).isTrue();
    }

    @NotNull
    private ProxyEntry createProxyEntry() {
        return ProxyEntry.Builder.newInstance().type(TEST_TYPE).build();
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

}