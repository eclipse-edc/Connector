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

import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderPushTransferDataFlowControllerTest {

    private final DataPlaneClient dataPlaneClient = mock();
    private ProviderPushTransferDataFlowController flowController;

    @BeforeEach
    void setUp() {
        var callbackUrlMock = mock(ControlApiUrl.class);
        var url = URI.create("http://localhost");
        when(callbackUrlMock.get()).thenReturn(url);
        flowController = new ProviderPushTransferDataFlowController(callbackUrlMock, dataPlaneClient);
    }

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(transferProcess(HTTP_PROXY))).isFalse();
        assertThat(flowController.canHandle(transferProcess("not-http-proxy"))).isTrue();
    }

    @Test
    void verifyReturnFailedResultIfTransferFails() {
        var errorMsg = "error";
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.transfer(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));

        var result = flowController.initiateFlow(transferProcess, Policy.Builder.newInstance().build());

        verify(dataPlaneClient).transfer(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void verifyTransferSuccess() {
        var request = createDataRequest();
        var source = testDataAddress();
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.transfer(any(DataFlowRequest.class))).thenReturn(StatusResult.success());

        var result = flowController.initiateFlow(transferProcess, Policy.Builder.newInstance().build());

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(DataFlowRequest.class);
        verify(dataPlaneClient).transfer(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.isTrackable()).isTrue();
        assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    @Test
    void verifyTransferSuccessWithAdditionalProperties() {
        var request = createDataRequest("test");
        var source = testDataAddress();
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.transfer(any(DataFlowRequest.class))).thenReturn(StatusResult.success());

        var result = flowController.initiateFlow(transferProcess, Policy.Builder.newInstance().build());

        assertThat(result.succeeded()).isTrue();
        var captor = ArgumentCaptor.forClass(DataFlowRequest.class);
        verify(dataPlaneClient).transfer(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.isTrackable()).isTrue();
        assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    private DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    private DataRequest createDataRequest() {
        return createDataRequest("test");
    }

    private DataRequest createDataRequest(String destinationType) {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("test-protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(destinationType)
                .build();
    }

    private TransferProcess transferProcess(String destinationType) {
        return TransferProcess.Builder.newInstance()
                .dataRequest(DataRequest.Builder.newInstance().destinationType(destinationType).build())
                .build();
    }
}
