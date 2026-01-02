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

package org.eclipse.edc.signaling.logic;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.signaling.port.DataPlaneSignalingClient;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DataPlaneSignalingFlowControllerTest {

    private static final String HTTP_DATA_PULL = "HttpData-PULL";
    private final DataPlaneSignalingClient dataPlaneClient = mock();
    private final ClientFactory clientFactory = mock();
    private final DataPlaneSelectorService selectorService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();

    private final DataPlaneSignalingFlowController flowController = new DataPlaneSignalingFlowController(
            () -> URI.create("http://localhost"), selectorService,
            "random", typeTransformerRegistry, clientFactory);

    @Nested
    class Prepare {

        @Test
        void shouldCallPrepareOnDataPlane() {
            var dataPlaneInstance = createDataPlaneInstance();
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(clientFactory.createClient(any())).thenReturn(dataPlaneClient);
            var flowResponseMessage = DataFlowResponseMessage.Builder.newInstance()
                    .dataAddress(createDspDataAddress())
                    .build();
            when(dataPlaneClient.prepare(any())).thenReturn(StatusResult.success(flowResponseMessage));
            when(typeTransformerRegistry.transform(isA(DataAddress.class), any())).thenReturn(Result.success(createDspDataAddress()));
            var response = DataFlowResponse.Builder.newInstance().dataPlaneId("dataPlaneId").dataAddress(testDataAddress()).build();
            when(typeTransformerRegistry.transform(isA(DataFlowResponseMessage.class), any())).thenReturn(Result.success(response));

            var result = flowController.prepare(transferProcess, policyBuilder().build());

            assertThat(result).isSucceeded().isSameAs(response);
            verify(dataPlaneClient).prepare(isA(DataFlowPrepareMessage.class));
        }

        @Test
        void shouldReturnFailure_whenNoDataPlaneIsFound() {
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.notFound("no data plane can provision this"));

            var result = flowController.prepare(transferProcess, policyBuilder().build());

            assertThat(result).isFailed();
            verifyNoInteractions(clientFactory);
        }

    }

    @Nested
    class Start {
        @Test
        void shouldSelectAndCallStartOnDataplane() {
            var policy = Policy.Builder.newInstance().assignee("participantId").build();
            var transferProcess = transferProcessBuilder()
                    .transferType(HTTP_DATA_PULL)
                    .contentDataAddress(testDataAddress())
                    .build();
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(clientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(typeTransformerRegistry.transform(isA(DataAddress.class), any())).thenReturn(Result.success(createDspDataAddress()));
            var response = DataFlowResponse.Builder.newInstance().dataPlaneId("dataPlaneId").dataAddress(testDataAddress()).build();
            when(typeTransformerRegistry.transform(isA(DataFlowResponseMessage.class), any())).thenReturn(Result.success(response));
            when(dataPlaneClient.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance()
                    .dataAddress(createDspDataAddress())
                    .build()));

            var result = flowController.start(transferProcess, policy);

            assertThat(result).isSucceeded().isSameAs(response);
        }

        @Test
        void shouldFail_whenNoDataplaneSelected() {
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(selectorService.select(any(), any())).thenReturn(ServiceResult.notFound("no dataplane found"));

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result).isFailed();
        }

        @Test
        void returnFailedResultIfTransferFails() {
            var errorMsg = "error";
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(dataPlaneClient.start(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(clientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(typeTransformerRegistry.transform(isA(DataAddress.class), any())).thenReturn(Result.success(createDspDataAddress()));

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            verify(dataPlaneClient).start(any());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
        }
    }

    @Nested
    class Terminate {

        @Test
        void shouldCallTerminateOnTheRightDataPlane() {
            var dataPlaneInstance = dataPlaneInstanceBuilder().id("dataPlaneId").build();
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("dataPlaneId")
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(clientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).terminate("transferProcessId");
            verify(clientFactory).createClient(dataPlaneInstance);
        }

        @Test
        void shouldFail_whenDataPlaneNotFound() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(clientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isFailed().detail().isEqualTo("not found");
        }

        @Test
        // a null dataPlaneId means that the flow has not been started so it can be considered as already terminated
        void shouldReturnSuccess_whenDataPlaneIdIsNull() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId(null)
                    .build();

            var result = flowController.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verifyNoInteractions(dataPlaneClient, clientFactory, selectorService);
        }
    }

    @Nested
    class TransferTypes {

        @Test
        void transferTypes_shouldReturnTypesForSpecifiedAsset() {

            var assetNoResponse = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PULL").build()
            )));

            var transferTypes = flowController.transferTypesFor(assetNoResponse);

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

    private TransferProcess.Builder transferProcessBuilder() {
        return TransferProcess.Builder.newInstance()
                .correlationId(UUID.randomUUID().toString())
                .protocol("test-protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .counterPartyAddress("test.connector.address")
                .transferType("transferType")
                .dataDestination(DataAddress.Builder.newInstance().type("test").build());
    }

    private Policy.Builder policyBuilder() {
        return Policy.Builder.newInstance();
    }

    private DataAddress buildResponseChannel() {
        return DataAddress.Builder.newInstance().type("Response").build();
    }

    private @NonNull DspDataAddress createDspDataAddress() {
        return DspDataAddress.Builder.newInstance().build();
    }
}
