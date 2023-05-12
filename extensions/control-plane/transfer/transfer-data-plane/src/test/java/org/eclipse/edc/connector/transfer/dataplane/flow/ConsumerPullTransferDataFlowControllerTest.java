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

package org.eclipse.edc.connector.transfer.dataplane.flow;

import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceCreationRequest;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerPullTransferDataFlowControllerTest {

    private String connectorId;
    private ConsumerPullTransferProxyResolver proxyResolverMock;
    private RemoteMessageDispatcherRegistry dispatcherRegistryMock;
    private ConsumerPullTransferEndpointDataReferenceService proxyReferenceServiceMock;
    private ConsumerPullTransferDataFlowController flowController;

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .endpoint("test.endpoint.url")
                .build();
    }

    private static DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    private static DataRequest createDataRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("ids-multipart")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(HTTP_PROXY)
                .build();
    }

    @BeforeEach
    void setUp() {
        connectorId = "test";
        proxyResolverMock = mock(ConsumerPullTransferProxyResolver.class);
        dispatcherRegistryMock = mock(RemoteMessageDispatcherRegistry.class);
        proxyReferenceServiceMock = mock(ConsumerPullTransferEndpointDataReferenceService.class);
        flowController = new ConsumerPullTransferDataFlowController(connectorId, proxyResolverMock, proxyReferenceServiceMock, dispatcherRegistryMock);
    }

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType(HTTP_PROXY).build(), null)).isTrue();
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType("not-http-proxy").build(), null)).isFalse();
    }

    @Test
    void verifySuccessfulProxyReferenceCreationAndDispatch() {
        var request = createDataRequest();
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();
        var proxyUrl = "proxy.test.url";

        var edrRequestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceMessage.class);
        var proxyCreationRequestCaptor = ArgumentCaptor.forClass(ConsumerPullTransferEndpointDataReferenceCreationRequest.class);

        when(dispatcherRegistryMock.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(proxyReferenceServiceMock.createProxyReference(any())).thenReturn(Result.success(edr));
        when(proxyResolverMock.resolveProxyUrl(dataAddress)).thenReturn(Result.success(proxyUrl));

        var result = flowController.initiateFlow(request, dataAddress, Policy.Builder.newInstance().build());

        verify(proxyReferenceServiceMock).createProxyReference(proxyCreationRequestCaptor.capture());
        verify(dispatcherRegistryMock).send(eq(Object.class), edrRequestCaptor.capture());
        verify(proxyResolverMock).resolveProxyUrl(any());

        assertThat(result.succeeded()).isTrue();

        var dataFlowResponse = result.getContent();
        assertThat(dataFlowResponse.getDataAddress()).isNotNull().satisfies(address -> {
            assertThat(address.getType()).isEqualTo(EndpointDataReference.EDR_SIMPLE_TYPE);
            assertThat(address.getProperty(EndpointDataReference.ENDPOINT)).isEqualTo(edr.getEndpoint());
            assertThat(address.getProperty(EndpointDataReference.AUTH_KEY)).isEqualTo(edr.getAuthKey());
            assertThat(address.getProperty(EndpointDataReference.ID)).isEqualTo(edr.getId());
            assertThat(address.getProperty(EndpointDataReference.AUTH_CODE)).isEqualTo(edr.getAuthCode());
            assertThat(address.getProperties()).containsAllEntriesOf(edr.getProperties());
        });

        var edrRequest = edrRequestCaptor.getValue();
        assertThat(edrRequest.getConnectorId()).isEqualTo(connectorId);
        assertThat(edrRequest.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(edrRequest.getCounterPartyAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(edrRequest.getEndpointDataReference()).isEqualTo(edr);

        var proxyCreationRequest = proxyCreationRequestCaptor.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(request.getId());
        assertThat(proxyCreationRequest.getContentAddress()).isEqualTo(dataAddress);
        assertThat(proxyCreationRequest.getProxyEndpoint()).isEqualTo(proxyUrl);
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(request.getContractId());
        assertThat(proxyCreationRequest.getProperties()).isEmpty();
    }

    @Test
    void verifyReturnFailedResultIfProxyUrlResolutionFails() {
        var request = createDataRequest();
        var dataAddress = testDataAddress();
        var errorMsg = "test-errormsg";

        when(proxyResolverMock.resolveProxyUrl(dataAddress)).thenReturn(Result.failure(errorMsg));

        var result = flowController.initiateFlow(request, dataAddress, Policy.Builder.newInstance().build());

        verify(dispatcherRegistryMock, never()).send(any(), any());
        verify(proxyResolverMock).resolveProxyUrl(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void verifyReturnFailedResultIfProxyCreationFails() {
        var request = createDataRequest();
        var dataAddress = testDataAddress();
        var errorMsg = "Test Error Message";
        var proxyUrl = "test.proxy.url";

        when(proxyResolverMock.resolveProxyUrl(dataAddress)).thenReturn(Result.success(proxyUrl));
        when(proxyReferenceServiceMock.createProxyReference(any())).thenReturn(Result.failure(errorMsg));

        var result = flowController.initiateFlow(request, dataAddress, Policy.Builder.newInstance().build());

        verify(proxyResolverMock).resolveProxyUrl(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }
}
