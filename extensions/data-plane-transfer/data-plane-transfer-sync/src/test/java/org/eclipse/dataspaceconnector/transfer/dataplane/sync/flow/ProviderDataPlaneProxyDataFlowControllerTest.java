package org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow;

import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.CONTRACT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderDataPlaneProxyDataFlowControllerTest {

    private static final String DATA_PLANE_ENDPOINT = "http://data-plane.com";

    private String connectorId;
    private RemoteMessageDispatcherRegistry dispatcherRegistryMock;
    private DataAddressResolver dataAddressResolverMock;
    private DataPlaneProxyManager proxyManagerMock;
    private ProviderDataPlaneProxyDataFlowController controller;

    @BeforeEach
    void setUp() {
        connectorId = "connector-test";
        dispatcherRegistryMock = mock(RemoteMessageDispatcherRegistry.class);
        dataAddressResolverMock = mock(DataAddressResolver.class);
        proxyManagerMock = mock(DataPlaneProxyManager.class);
        controller = new ProviderDataPlaneProxyDataFlowController(connectorId, dispatcherRegistryMock, dataAddressResolverMock, proxyManagerMock);
    }

    @Test
    void verifyCanHandle() {
        assertThat(controller.canHandle(createDataRequest("HttpProxy"))).isTrue();
        assertThat(controller.canHandle(createDataRequest("dummy"))).isFalse();
    }

    @Test
    void verifyInitiateFlowSuccess() {
        var request = createDataRequest("HttpProxy");
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();

        var edrRequestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceMessage.class);
        var proxyCreationRequestCaptor = ArgumentCaptor.forClass(DataPlaneProxyCreationRequest.class);

        when(dispatcherRegistryMock.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(proxyManagerMock.createProxy(any())).thenReturn(Result.success(edr));
        when(dataAddressResolverMock.resolveForAsset(request.getAssetId())).thenReturn(dataAddress);

        var result = controller.initiateFlow(request);

        verify(proxyManagerMock, times(1)).createProxy(proxyCreationRequestCaptor.capture());
        verify(dataAddressResolverMock, times(1)).resolveForAsset(request.getAssetId());
        verify(dispatcherRegistryMock, times(1))
                .send(ArgumentCaptor.forClass(Class.class).capture(), edrRequestCaptor.capture(), ArgumentCaptor.forClass(MessageContext.class).capture());

        assertThat(result.succeeded()).isTrue();
        var edrRequest = edrRequestCaptor.getValue();
        assertThat(edrRequest.getConnectorId()).isEqualTo(connectorId);
        assertThat(edrRequest.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(edrRequest.getConnectorAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(edrRequest.getEndpointDataReference()).isEqualTo(edr);

        var proxyCreationRequest = proxyCreationRequestCaptor.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(request.getId());
        assertThat(proxyCreationRequest.getAddress()).isEqualTo(dataAddress);
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(request.getContractId());
        assertThat(proxyCreationRequest.getProperties()).containsOnlyKeys(CONTRACT_ID);
    }

    @Test
    void proxyCreationFails_shouldReturnFailedResult() {
        var request = createDataRequest("HttpProxy");
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();

        when(dataAddressResolverMock.resolveForAsset(request.getAssetId())).thenReturn(dataAddress);
        when(proxyManagerMock.createProxy(any())).thenReturn(Result.failure("error"));

        var result = controller.initiateFlow(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Failed to generate proxy: error");
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .id("id-test")
                .endpoint("http://example.com")
                .build();
    }

    private static DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("dummy").build();
    }

    private static DataRequest createDataRequest(String destinationType) {
        return DataRequest.Builder.newInstance()
                .id("1")
                .protocol("protocol-test")
                .contractId("contract-test")
                .assetId("asset-test")
                .connectorAddress("http://consumer-connector.com")
                .processId("process-test")
                .destinationType(destinationType)
                .build();
    }
}