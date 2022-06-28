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

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferProxyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderDataPlaneProxyDataFlowControllerTest {

    private static final Faker FAKER = new Faker();

    private String connectorId;
    private DataPlaneTransferProxyResolver proxyResolver;
    private RemoteMessageDispatcherRegistry dispatcherRegistryMock;
    private DataPlaneTransferProxyReferenceService proxyReferenceServiceMock;
    private ProviderDataPlaneProxyDataFlowController controller;

    @BeforeEach
    void setUp() {
        connectorId = FAKER.internet().uuid();
        proxyResolver = mock(DataPlaneTransferProxyResolver.class);
        dispatcherRegistryMock = mock(RemoteMessageDispatcherRegistry.class);
        proxyReferenceServiceMock = mock(DataPlaneTransferProxyReferenceService.class);
        controller = new ProviderDataPlaneProxyDataFlowController(connectorId, proxyResolver, dispatcherRegistryMock, proxyReferenceServiceMock);
    }

    @Test
    void verifyCanHandle() {
        var contentAddress = DataAddress.Builder.newInstance().type(HTTP_PROXY).build();

        assertThat(controller.canHandle(createDataRequest(HTTP_PROXY), contentAddress)).isTrue();
        assertThat(controller.canHandle(createDataRequest(FAKER.internet().uuid()), contentAddress)).isFalse();
    }

    @Test
    void verifyInitiateFlowSuccess() {
        var request = createDataRequest(HTTP_PROXY);
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();
        var proxyUrl = FAKER.internet().url();

        var edrRequestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceMessage.class);
        var proxyCreationRequestCaptor = ArgumentCaptor.forClass(DataPlaneTransferProxyCreationRequest.class);

        when(dispatcherRegistryMock.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(proxyReferenceServiceMock.createProxyReference(any())).thenReturn(Result.success(edr));
        when(proxyResolver.resolveProxyUrl(dataAddress)).thenReturn(Result.success(proxyUrl));

        var result = controller.initiateFlow(request, dataAddress, policy);

        verify(proxyReferenceServiceMock).createProxyReference(proxyCreationRequestCaptor.capture());
        verify(dispatcherRegistryMock).send(eq(Object.class), edrRequestCaptor.capture(), any(MessageContext.class));
        verify(proxyResolver).resolveProxyUrl(any());

        assertThat(result.succeeded()).isTrue();
        var edrRequest = edrRequestCaptor.getValue();
        assertThat(edrRequest.getConnectorId()).isEqualTo(connectorId);
        assertThat(edrRequest.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(edrRequest.getConnectorAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(edrRequest.getEndpointDataReference()).isEqualTo(edr);

        var proxyCreationRequest = proxyCreationRequestCaptor.getValue();
        assertThat(proxyCreationRequest.getId()).isEqualTo(request.getId());
        assertThat(proxyCreationRequest.getContentAddress()).isEqualTo(dataAddress);
        assertThat(proxyCreationRequest.getProxyEndpoint()).isEqualTo(proxyUrl);
        assertThat(proxyCreationRequest.getContractId()).isEqualTo(request.getContractId());
        assertThat(proxyCreationRequest.getProperties()).isEmpty();
    }

    @Test
    void proxyUrlResolutionFails_shouldReturnFailedResult() {
        var request = createDataRequest(HTTP_PROXY);
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = testDataAddress();
        var errorMsg = FAKER.lorem().sentence();

        when(proxyResolver.resolveProxyUrl(dataAddress)).thenReturn(Result.failure(errorMsg));

        var result = controller.initiateFlow(request, dataAddress, policy);

        verify(dispatcherRegistryMock, never()).send(any(), any(), any());
        verify(proxyResolver).resolveProxyUrl(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void proxyCreationFails_shouldReturnFailedResult() {
        var request = createDataRequest(HTTP_PROXY);
        var policy = Policy.Builder.newInstance().build();
        var dataAddress = testDataAddress();
        var errorMsg = FAKER.lorem().sentence();
        var proxyUrl = FAKER.internet().url();

        when(proxyResolver.resolveProxyUrl(dataAddress)).thenReturn(Result.success(proxyUrl));
        when(proxyReferenceServiceMock.createProxyReference(any())).thenReturn(Result.failure(errorMsg));

        var result = controller.initiateFlow(request, dataAddress, policy);

        verify(proxyResolver).resolveProxyUrl(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .endpoint(FAKER.internet().url())
                .build();
    }

    private static DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build();
    }

    private static DataRequest createDataRequest(String destinationType) {
        return DataRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .protocol(FAKER.lorem().word())
                .contractId(FAKER.internet().uuid())
                .assetId(FAKER.internet().uuid())
                .connectorAddress(FAKER.internet().url())
                .processId(FAKER.internet().uuid())
                .destinationType(destinationType)
                .build();
    }
}
