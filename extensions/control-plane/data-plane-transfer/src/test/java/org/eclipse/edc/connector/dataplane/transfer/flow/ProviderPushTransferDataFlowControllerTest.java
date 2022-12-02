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

package org.eclipse.edc.connector.dataplane.transfer.flow;

import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.transfer.spi.DataPlaneTransferConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderPushTransferDataFlowControllerTest {

    private DataPlaneClient dataPlaneClientMock;
    private ProviderPushTransferDataFlowController flowController;


    @BeforeEach
    void setUp() throws MalformedURLException {
        var callbackUrlMock = mock(ControlPlaneApiUrl.class);
        var url = new URL("http://localhost");
        when(callbackUrlMock.get()).thenReturn(url);
        dataPlaneClientMock = mock(DataPlaneClient.class);
        flowController = new ProviderPushTransferDataFlowController(callbackUrlMock, dataPlaneClientMock);
    }

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType(HTTP_PROXY).build(), null)).isFalse();
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType("not-http-proxy").build(), null)).isTrue();
    }

    @Test
    void verifyReturnFailedResultIfTransferFails() {
        var errorMsg = "error";
        var request = createDataRequest();

        when(dataPlaneClientMock.transfer(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));

        var result = flowController.initiateFlow(request, testDataAddress(), Policy.Builder.newInstance().build());

        verify(dataPlaneClientMock).transfer(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void verifyTransferSuccess() {
        var request = createDataRequest();
        var source = testDataAddress();

        when(dataPlaneClientMock.transfer(any(DataFlowRequest.class))).thenReturn(StatusResult.success());

        var result = flowController.initiateFlow(request, source, Policy.Builder.newInstance().build());

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(DataFlowRequest.class);
        verify(dataPlaneClientMock).transfer(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.isTrackable()).isTrue();
        assertThat(captured.getProcessId()).isEqualTo(request.getProcessId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    @Test
    void verifyTransferSuccessWithAdditionalProperties() {
        var properties = Map.of("foo", "bar", "hello", "world");
        var request = createDataRequest("test", properties);
        var source = testDataAddress();

        when(dataPlaneClientMock.transfer(any(DataFlowRequest.class))).thenReturn(StatusResult.success());

        var result = flowController.initiateFlow(request, source, Policy.Builder.newInstance().build());

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(DataFlowRequest.class);
        verify(dataPlaneClientMock).transfer(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.isTrackable()).isTrue();
        assertThat(captured.getProcessId()).isEqualTo(request.getProcessId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getProperties()).containsExactlyInAnyOrderEntriesOf(properties);
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    private static DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    private static DataRequest createDataRequest() {
        return createDataRequest("test", Map.of());
    }

    private static DataRequest createDataRequest(String destinationType, Map<String, String> properties) {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("test-protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(destinationType)
                .properties(properties)
                .build();
    }
}
