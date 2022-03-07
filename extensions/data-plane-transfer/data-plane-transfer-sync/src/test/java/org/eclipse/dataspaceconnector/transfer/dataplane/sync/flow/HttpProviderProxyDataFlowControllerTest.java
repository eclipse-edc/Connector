package org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow;

import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.EXPIRATION;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.TOKEN;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpProviderProxyDataFlowControllerTest {

    private String connectorId;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private HttpProviderProxyDataFlowController controller;

    @BeforeEach
    void setUp() {
        connectorId = "connector-test";
        dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
        controller = new HttpProviderProxyDataFlowController(connectorId, dispatcherRegistry);
    }

    @Test
    void verifyCanHandle() {
        var address = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://provider-connector.com")
                .property(TOKEN, "token-test")
                .property(EXPIRATION, Long.toString(Instant.now().plusSeconds(100).getEpochSecond()))
                .build();

        var request = createDataRequest(address);

        assertThat(controller.canHandle(request)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("badRequests")
    void verifyCannotHandle(DataRequest request) {
        assertThat(controller.canHandle(request)).isFalse();
    }

    @Test
    void verifyInitiateFlow() {
        var request = createDataRequest(TYPE);
        var responseTypeCaptor = ArgumentCaptor.forClass(Class.class);
        var edrRequestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceRequest.class);
        var msgContextCaptor = ArgumentCaptor.forClass(MessageContext.class);
        when(dispatcherRegistry.send(responseTypeCaptor.capture(), edrRequestCaptor.capture(), msgContextCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var result = controller.initiateFlow(request);

        verify(dispatcherRegistry, times(1)).send(any(), any(), any());

        assertThat(result.succeeded()).isTrue();
        assertThat(edrRequestCaptor.getValue()).satisfies(edrRequest -> {
            assertThat(edrRequest.getConnectorId()).isEqualTo(connectorId);
            assertThat(edrRequest.getProtocol()).isEqualTo(request.getProtocol());
            assertThat(edrRequest.getConnectorAddress()).isEqualTo(request.getConnectorAddress());
            assertThat(edrRequest.getEndpointDataReference()).satisfies(edr -> {
                assertThat(edr.getContractId()).isEqualTo(request.getContractId());
                assertThat(edr.getExpirationEpochSeconds()).isEqualTo(Long.parseLong(request.getDataDestination().getProperty(EXPIRATION)));
                assertThat(edr.getAuthKey()).isEqualTo("Authorization");
                assertThat(edr.getAuthCode()).isEqualTo(request.getDataDestination().getProperty(TOKEN));
                assertThat(edr.getCorrelationId()).isEqualTo(request.getId());
                assertThat(edr.getAddress()).isEqualTo(request.getDataDestination().getProperty(ENDPOINT));
            });
        });
    }

    /**
     * Serves some bad request (missing parameters, unhandled types...) that are used to verify
     * the controller says it cannot handle them.
     */
    private static Stream<Arguments> badRequests() {
        var unhandledType = DataAddress.Builder.newInstance()
                .type("dummy")
                .property(ENDPOINT, "http://provider-connector.com")
                .property(TOKEN, "token-test")
                .property(EXPIRATION, Long.toString(Instant.now().plusSeconds(100).getEpochSecond()))
                .build();

        var missingEndpoint = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(TOKEN, "token-test")
                .property(EXPIRATION, Long.toString(Instant.now().plusSeconds(100).getEpochSecond()))
                .build();

        var missingToken = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://provider-connector.com")
                .property(EXPIRATION, Long.toString(Instant.now().plusSeconds(100).getEpochSecond()))
                .build();

        var missingExpiration = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ENDPOINT, "http://provider-connector.com")
                .property(TOKEN, "token-test")
                .build();

        return Stream.of(
                Arguments.of(createDataRequest(unhandledType)),
                Arguments.of(createDataRequest(missingEndpoint)),
                Arguments.of(createDataRequest(missingExpiration)),
                Arguments.of(createDataRequest(missingToken))
        );
    }

    private static DataRequest createDataRequest(DataAddress destDataAddress) {
        return DataRequest.Builder.newInstance()
                .id("1")
                .protocol("protocol-test")
                .contractId("contract-test")
                .connectorAddress("http://consumer-connector.com")
                .processId("process-test")
                .dataDestination(destDataAddress)
                .build();
    }

    private static DataRequest createDataRequest(String destDataAddressType) {
        return DataRequest.Builder.newInstance()
                .id("1")
                .protocol("protocol-test")
                .contractId("contract-test")
                .connectorAddress("http://consumer-connector.com")
                .processId("process-test")
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(destDataAddressType)
                        .property(ENDPOINT, "http://provider-connector.com")
                        .property(TOKEN, "token-test")
                        .property(EXPIRATION, Long.toString(Instant.now().plusSeconds(100).getEpochSecond()))
                        .build())
                .build();
    }
}