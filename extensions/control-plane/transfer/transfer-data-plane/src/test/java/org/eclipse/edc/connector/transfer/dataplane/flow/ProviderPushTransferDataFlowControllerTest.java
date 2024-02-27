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

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderPushTransferDataFlowControllerTest {

    private static final String HTTP_DATA_PULL = "HttpData-PULL";
    private final DataPlaneClient dataPlaneClient = mock();
    private final DataPlaneClientFactory dataPlaneClientFactory = mock();
    private final DataPlaneSelectorService selectorService = mock();
    private final ProviderPushTransferDataFlowController flowController =
            new ProviderPushTransferDataFlowController(() -> URI.create("http://localhost"), selectorService, dataPlaneClientFactory);

    @NotNull
    private static DataPlaneInstance.Builder dataPlaneInstanceBuilder() {
        return DataPlaneInstance.Builder.newInstance().url("http://any");
    }

    @Test
    void canHandle() {
        assertThat(flowController.canHandle(transferProcess(HTTP_PROXY))).isFalse();
        assertThat(flowController.canHandle(transferProcess(HTTP_PROXY))).isFalse();
        assertThat(flowController.canHandle(transferProcess(HTTP_DATA_PULL, HTTP_DATA_PULL))).isFalse();
        assertThat(flowController.canHandle(transferProcess("not-http-proxy"))).isTrue();
    }

    @Test
    void initiateFlow_transferSuccess() {
        var request = createDataRequest();
        var source = testDataAddress();
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(mock(DataFlowResponseMessage.class)));
        var dataPlaneInstance = createDataPlaneInstance();
        when(selectorService.select(any(), any())).thenReturn(dataPlaneInstance);
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

        var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

        assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataPlaneId).isEqualTo(dataPlaneInstance.getId());
        var captor = ArgumentCaptor.forClass(DataFlowStartMessage.class);
        verify(dataPlaneClient).start(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    @Test
    void initiateFlow_transferSuccess_withoutDataPlane() {
        var request = createDataRequest();
        var source = testDataAddress();
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(mock(DataFlowResponseMessage.class)));
        when(selectorService.select(any(), any())).thenReturn(null);
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

        var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

        assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataPlaneId).isNull();
        var captor = ArgumentCaptor.forClass(DataFlowStartMessage.class);
        verify(dataPlaneClient).start(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
        assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
        assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(request.getDataDestination());
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getCallbackAddress()).isNotNull();
    }

    @Test
    void initiateFlow_returnFailedResultIfTransferFails() {
        var errorMsg = "error";
        var transferProcess = TransferProcess.Builder.newInstance()
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();

        when(dataPlaneClient.start(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));
        var dataPlaneInstance = createDataPlaneInstance();
        when(selectorService.select(any(), any())).thenReturn(dataPlaneInstance);
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

        var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

        verify(dataPlaneClient).start(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    @Test
    void terminate_shouldCallTerminate() {
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("transferProcessId")
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .build();
        when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
        var dataPlaneInstance = createDataPlaneInstance();
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
        when(selectorService.getAll()).thenReturn(List.of(dataPlaneInstance));

        var result = flowController.terminate(transferProcess);

        assertThat(result).isSucceeded();
        verify(dataPlaneClient).terminate("transferProcessId");
    }

    @Test
    void terminate_shouldCallTerminateOnTheRightDataPlane() {
        var dataPlaneInstance = createDataPlaneInstance();
        var mockedDataPlane = mock(DataPlaneInstance.class);
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("transferProcessId")
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .dataPlaneId(dataPlaneInstance.getId())
                .build();
        when(mockedDataPlane.getId()).thenReturn("notValidId");
        when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
        when(selectorService.getAll()).thenReturn(List.of(dataPlaneInstance, mockedDataPlane));

        var result = flowController.terminate(transferProcess);

        assertThat(result).isSucceeded();
        verify(dataPlaneClient).terminate("transferProcessId");
        verify(mockedDataPlane).getId();
    }

    @Test
    void terminate_shouldFail_withInvalidDataPlaneId() {
        var dataPlaneInstance = createDataPlaneInstance();
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("transferProcessId")
                .dataRequest(createDataRequest())
                .contentDataAddress(testDataAddress())
                .dataPlaneId("invalid")
                .build();
        when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
        when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
        when(selectorService.getAll()).thenReturn(List.of(dataPlaneInstance));

        var result = flowController.terminate(transferProcess);

        assertThat(result).isFailed().detail().contains("Failed to select the data plane for terminating the transfer process");
    }

    @Test
    void transferTypes_shouldReturnTypesForSpecifiedAsset() {
        when(selectorService.getAll()).thenReturn(List.of(
                dataPlaneInstanceBuilder().allowedSourceType("TargetSrc").allowedDestType("TargetDest").build(),
                dataPlaneInstanceBuilder().allowedSourceType("TargetSrc").allowedDestType("AnotherTargetDest").build(),
                dataPlaneInstanceBuilder().allowedSourceType("AnotherSrc").allowedDestType("ThisWontBeListed").build()
        ));
        var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();

        var transferTypes = flowController.transferTypesFor(asset);

        assertThat(transferTypes).containsExactly("TargetDest-PUSH", "AnotherTargetDest-PUSH");
    }

    private DataPlaneInstance createDataPlaneInstance() {
        return dataPlaneInstanceBuilder().build();
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
        return transferProcess(destinationType, null);
    }

    private TransferProcess transferProcess(String destinationType, String transferType) {
        return TransferProcess.Builder.newInstance()
                .transferType(transferType)
                .dataRequest(DataRequest.Builder.newInstance().destinationType(destinationType).build())
                .build();
    }
}
