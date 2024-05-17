/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataplane.flow;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.FlowTypeExtractor;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataPlaneSignalingFlowControllerTest {

    private static final String HTTP_DATA_PULL = "HttpData-PULL";
    private final DataPlaneClient dataPlaneClient = mock();
    private final DataPlaneClientFactory dataPlaneClientFactory = mock();
    private final DataPlaneSelectorService selectorService = mock();
    private final DataFlowPropertiesProvider propertiesProvider = mock();
    private final FlowTypeExtractor flowTypeExtractor = mock();

    private final DataPlaneSignalingFlowController flowController = new DataPlaneSignalingFlowController(
            () -> URI.create("http://localhost"), selectorService, propertiesProvider, dataPlaneClientFactory,
            "random", flowTypeExtractor);

    @Nested
    class CanHandle {
        @Test
        void shouldReturnTrue_whenFlowTypeIsValid() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PUSH));
            var transferProcess = transferProcess("Custom", "Valid-PUSH");

            var result = flowController.canHandle(transferProcess);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenFlowTypeIsNotValid() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));
            var transferProcess = transferProcess("Custom", "Invalid-ANY");

            var result = flowController.canHandle(transferProcess);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class InitiateFlow {
        @Test
        void transferSuccess() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PULL));
            var source = testDataAddress();
            var policy = Policy.Builder.newInstance().assignee("participantId").build();
            var transferProcess = transferProcessBuilder()
                    .transferType("transferType")
                    .contentDataAddress(testDataAddress())
                    .build();

            var customProperties = Map.of("foo", "bar");
            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(customProperties));
            when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(mock(DataFlowResponseMessage.class)));
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), anyString(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

            var result = flowController.start(transferProcess, policy);

            assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataPlaneId).isEqualTo(dataPlaneInstance.getId());
            var captor = ArgumentCaptor.forClass(DataFlowStartMessage.class);
            verify(dataPlaneClient).start(captor.capture());
            var captured = captor.getValue();
            assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
            assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
            assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(transferProcess.getDataDestination());
            assertThat(captured.getParticipantId()).isEqualTo(policy.getAssignee());
            assertThat(captured.getAgreementId()).isEqualTo(transferProcess.getContractId());
            assertThat(captured.getAssetId()).isEqualTo(transferProcess.getAssetId());
            assertThat(captured.getFlowType()).isEqualTo(FlowType.PULL);
            assertThat(captured.getProperties()).containsAllEntriesOf(customProperties);
            assertThat(captured.getCallbackAddress()).isNotNull();
        }

        @Test
        void transferSuccess_withReturnedDataAddress() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PULL));
            var policy = Policy.Builder.newInstance().assignee("participantId").build();
            var transferProcess = transferProcessBuilder()
                    .transferType(HTTP_DATA_PULL)
                    .contentDataAddress(testDataAddress())
                    .build();

            var response = mock(DataFlowResponseMessage.class);
            when(response.getDataAddress()).thenReturn(DataAddress.Builder.newInstance().type("type").build());
            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of()));
            when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(response));
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), eq(HTTP_DATA_PULL), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

            var result = flowController.start(transferProcess, policy);

            assertThat(result).isSucceeded()
                    .satisfies(dataFlowResponse -> {
                        assertThat(dataFlowResponse.getDataPlaneId()).isEqualTo(dataPlaneInstance.getId());
                        assertThat(dataFlowResponse.getDataAddress()).isNotNull();
                    });
        }

        @Test
        void transferSuccess_withoutDataPlane() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PULL));
            var source = testDataAddress();
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of()));
            when(dataPlaneClient.start(any(DataFlowStartMessage.class))).thenReturn(StatusResult.success(mock(DataFlowResponseMessage.class)));
            when(selectorService.select(any(), anyString(), any())).thenReturn(ServiceResult.notFound("no dataplane found"));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataPlaneId).isNull();
            var captor = ArgumentCaptor.forClass(DataFlowStartMessage.class);
            verify(dataPlaneClient).start(captor.capture());
            var captured = captor.getValue();
            assertThat(captured.getProcessId()).isEqualTo(transferProcess.getId());
            assertThat(captured.getSourceDataAddress()).usingRecursiveComparison().isEqualTo(source);
            assertThat(captured.getDestinationDataAddress()).usingRecursiveComparison().isEqualTo(transferProcess.getDataDestination());
            assertThat(captured.getProperties()).isEmpty();
            assertThat(captured.getCallbackAddress()).isNotNull();
        }

        @Test
        void invalidTransferType() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, "error"));
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType("invalid")
                    .build();

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result).isFailed().messages().containsOnly("error");
        }

        @Test
        void returnFailedResult_whenPropertiesResolveFails() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PULL));
            var errorMsg = "error";
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));
            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
        }

        @Test
        void returnFailedResultIfTransferFails() {
            when(flowTypeExtractor.extract(any())).thenReturn(StatusResult.success(FlowType.PULL));
            var errorMsg = "error";
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of()));
            when(dataPlaneClient.start(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), anyString(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            verify(dataPlaneClient).start(any());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
        }
    }

    @Nested
    class Terminate {
        @Test
        void shouldCallTerminate() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            var dataPlaneInstance = createDataPlaneInstance();
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance)));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).terminate("transferProcessId");
        }

        @Test
        void shouldCallTerminateOnTheRightDataPlane() {
            var dataPlaneInstance = createDataPlaneInstance();
            var mockedDataPlane = mock(DataPlaneInstance.class);
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId(dataPlaneInstance.getId())
                    .build();
            when(mockedDataPlane.getId()).thenReturn("notValidId");
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance, mockedDataPlane)));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).terminate("transferProcessId");
            verify(mockedDataPlane).getId();
        }

        @Test
        void shouldFail_withInvalidDataPlaneId() {
            var dataPlaneInstance = createDataPlaneInstance();
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance)));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isFailed().detail().contains("Failed to select the data plane for terminating the transfer process");
        }

        @Test
        void shouldFail_whenCannotGetDataplaneInstances() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(selectorService.getAll()).thenReturn(ServiceResult.unexpected("error"));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class Suspend {

        @Test
        void shouldCallTerminate() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .build();
            when(dataPlaneClient.suspend(any())).thenReturn(StatusResult.success());
            var dataPlaneInstance = createDataPlaneInstance();
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance)));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).suspend("transferProcessId");
        }

        @Test
        void shouldCallTerminateOnTheRightDataPlane() {
            var dataPlaneInstance = createDataPlaneInstance();
            var mockedDataPlane = mock(DataPlaneInstance.class);
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId(dataPlaneInstance.getId())
                    .build();
            when(mockedDataPlane.getId()).thenReturn("notValidId");
            when(dataPlaneClient.suspend(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance, mockedDataPlane)));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).suspend("transferProcessId");
            verify(mockedDataPlane).getId();
        }

        @Test
        void shouldFail_withInvalidDataPlaneId() {
            var dataPlaneInstance = createDataPlaneInstance();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(dataPlaneClient.suspend(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(dataPlaneInstance)));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isFailed().detail().contains("Failed to select the data plane for suspending the transfer process");
        }

        @Test
        void shouldFail_whenCannotGetDataplaneInstances() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(selectorService.getAll()).thenReturn(ServiceResult.unexpected("error"));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isFailed();
        }
    }

    @Nested
    class TransferTypes {

        @Test
        void transferTypes_shouldReturnTypesForSpecifiedAsset() {
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH").allowedSourceType("TargetSrc").allowedDestType("TargetDest").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PULL").allowedSourceType("TargetSrc").allowedDestType("AnotherTargetDest").build(),
                    dataPlaneInstanceBuilder().allowedSourceType("AnotherSrc").allowedDestType("ThisWontBeListed").build()
            )));
            var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();

            var transferTypes = flowController.transferTypesFor(asset);

            assertThat(transferTypes).containsExactly("Custom-PUSH", "Custom-PULL");
        }

        @Test
        void shouldReturnEmptyList_whenCannotGetDataplaneInstances() {
            when(selectorService.getAll()).thenReturn(ServiceResult.unexpected("error"));
            var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();

            var transferTypes = flowController.transferTypesFor(asset);

            assertThat(transferTypes).isEmpty();
        }
    }

    @NotNull
    private DataPlaneInstance.Builder dataPlaneInstanceBuilder() {
        return DataPlaneInstance.Builder.newInstance().url("http://any");
    }

    private DataPlaneInstance createDataPlaneInstance() {
        return dataPlaneInstanceBuilder().build();
    }

    private DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    private TransferProcess transferProcess(String destinationType, String transferType) {
        return TransferProcess.Builder.newInstance()
                .transferType(transferType)
                .dataDestination(DataAddress.Builder.newInstance().type(destinationType).build())
                .build();
    }

    private TransferProcess.Builder transferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .correlationId(UUID.randomUUID().toString())
                .protocol("test-protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .counterPartyAddress("test.connector.address")
                .dataDestination(DataAddress.Builder.newInstance().type("test").build());
    }

}
