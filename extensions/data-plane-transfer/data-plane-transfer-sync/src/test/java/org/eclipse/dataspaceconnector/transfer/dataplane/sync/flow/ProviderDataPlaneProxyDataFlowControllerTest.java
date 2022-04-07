/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow;

import org.eclipse.dataspaceconnector.policy.model.Policy;
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
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferType.SYNC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderDataPlaneProxyDataFlowControllerTest {

    private String connectorId;
    private RemoteMessageDispatcherRegistry dispatcherRegistryMock;
    private DataPlaneProxyManager proxyManagerMock;
    private ProviderDataPlaneProxyDataFlowController controller;

    @BeforeEach
    void setUp() {
        connectorId = "connector-test";
        dispatcherRegistryMock = mock(RemoteMessageDispatcherRegistry.class);
        proxyManagerMock = mock(DataPlaneProxyManager.class);
        controller = new ProviderDataPlaneProxyDataFlowController(connectorId, dispatcherRegistryMock, proxyManagerMock);
    }

    @Test
    void verifyCanHandle() {
        var contentAddress = DataAddress.Builder.newInstance().type(SYNC).build();

        assertThat(controller.canHandle(createDataRequest(SYNC), contentAddress)).isTrue();
        assertThat(controller.canHandle(createDataRequest("dummy"), contentAddress)).isFalse();
    }

    @Test
    void verifyInitiateFlowSuccess() {
        var request = createDataRequest(SYNC);
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();

        var edrRequestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceMessage.class);
        var proxyCreationRequestCaptor = ArgumentCaptor.forClass(DataPlaneProxyCreationRequest.class);

        when(dispatcherRegistryMock.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(proxyManagerMock.createProxy(any())).thenReturn(Result.success(edr));

        var result = controller.initiateFlow(request, dataAddress, policy);

        verify(proxyManagerMock, times(1)).createProxy(proxyCreationRequestCaptor.capture());
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
        var request = createDataRequest(SYNC);
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();

        when(proxyManagerMock.createProxy(any())).thenReturn(Result.failure("error"));

        var result = controller.initiateFlow(request, dataAddress, policy);

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
