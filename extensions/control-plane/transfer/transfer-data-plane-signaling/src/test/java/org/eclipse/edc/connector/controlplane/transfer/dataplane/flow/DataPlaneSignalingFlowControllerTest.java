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
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DataPlaneSignalingFlowControllerTest {

    private static final String HTTP_DATA_PULL = "HttpData-PULL";
    private final DataPlaneClient dataPlaneClient = mock();
    private final DataPlaneClientFactory dataPlaneClientFactory = mock();
    private final DataPlaneSelectorService selectorService = mock();
    private final DataFlowPropertiesProvider propertiesProvider = mock();
    private final TransferTypeParser transferTypeParser = mock();

    private final DataPlaneSignalingFlowController flowController = new DataPlaneSignalingFlowController(
            () -> URI.create("http://localhost"), selectorService, propertiesProvider, dataPlaneClientFactory,
            "random", transferTypeParser);

    @Nested
    class CanHandle {
        @Test
        void shouldReturnTrue_whenFlowTypeIsValid() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PUSH)));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("Valid-PUSH")
                    .dataDestination(DataAddress.Builder.newInstance().type("Custom").build())
                    .build();

            var result = flowController.canHandle(transferProcess);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenFlowTypeIsNotValid() {
            when(transferTypeParser.parse(any())).thenReturn(Result.failure("cannot parse"));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("Invalid-ANY")
                    .dataDestination(DataAddress.Builder.newInstance().type("Custom").build())
                    .build();

            var result = flowController.canHandle(transferProcess);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class Provision {

        @Test
        void shouldCallPrepareOnDataPlane() {
            var dataPlaneInstance = createDataPlaneInstance();
            var transferProcess = transferProcessBuilder().build();
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("any", FlowType.PUSH)));
            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(emptyMap()));
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            var newDestinationAddress = DataAddress.Builder.newInstance().type("any").build();
            var flowResponseMessage = DataFlowResponseMessage.Builder.newInstance()
                    .dataAddress(newDestinationAddress)
                    .provisioning(true)
                    .build();
            when(dataPlaneClient.provision(any())).thenReturn(StatusResult.success(flowResponseMessage));

            var result = flowController.provision(transferProcess, policyBuilder().build());

            assertThat(result).isSucceeded().satisfies(dataFlowResponse -> {
                assertThat(dataFlowResponse.getDataPlaneId()).isEqualTo(dataPlaneInstance.getId());
                assertThat(dataFlowResponse.getDataAddress()).isSameAs(newDestinationAddress);
                assertThat(dataFlowResponse.isProvisioning()).isTrue();
            });
            verify(dataPlaneClient).provision(isA(DataFlowProvisionMessage.class));
        }

        @Test
        void shouldReturnFailure_whenNoDataPlaneIsFound() {
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.notFound("no data plane can provision this"));

            var result = flowController.provision(transferProcess, policyBuilder().build());

            assertThat(result).isFailed();
            verifyNoInteractions(dataPlaneClientFactory);
        }

        @Test
        void shouldReturnSuccess_whenDataPlaneSelectionFailsWithUnexpectedError() {
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.unexpected("unexpected error"));

            var result = flowController.provision(transferProcess, policyBuilder().build());

            assertThat(result).isFailed();
        }

        @Test
        void shouldReturnFailure_whenTransferTypeIsNotValid() {
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(createDataPlaneInstance()));
            when(transferTypeParser.parse(any())).thenReturn(Result.failure("transferType error"));

            var result = flowController.provision(transferProcess, policyBuilder().build());

            assertThat(result).isFailed().detail().isEqualTo("transferType error");
        }

        @Test
        void shouldReturnFailure_whenPropertiesProviderFails() {
            var transferProcess = transferProcessBuilder().build();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(createDataPlaneInstance()));
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("any", FlowType.PUSH)));
            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, "propertiesProvider error"));

            var result = flowController.provision(transferProcess, policyBuilder().build());

            assertThat(result).isFailed().detail().isEqualTo("propertiesProvider error");
        }
    }

    @Nested
    class InitiateFlow {
        @Test
        void transferSuccess() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PULL)));
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
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
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
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PULL)));
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
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);

            var result = flowController.start(transferProcess, policy);

            assertThat(result).isSucceeded()
                    .satisfies(dataFlowResponse -> {
                        assertThat(dataFlowResponse.getDataPlaneId()).isEqualTo(dataPlaneInstance.getId());
                        assertThat(dataFlowResponse.getDataAddress()).isNotNull();
                    });
        }

        @Test
        void shouldFail_whenNoDataplaneSelected() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PULL)));
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of()));
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.notFound("no dataplane found"));

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result).isFailed();
        }

        @Test
        void invalidTransferType() {
            when(transferTypeParser.parse(any())).thenReturn(Result.failure("cannot parse"));
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType("invalid")
                    .build();

            var result = flowController.start(transferProcess, Policy.Builder.newInstance().build());

            assertThat(result).isFailed().detail().contains("cannot parse");
        }

        @Test
        void returnFailedResult_whenPropertiesResolveFails() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PULL)));
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
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PULL)));
            var errorMsg = "error";
            var transferProcess = transferProcessBuilder()
                    .contentDataAddress(testDataAddress())
                    .transferType(HTTP_DATA_PULL)
                    .build();

            when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of()));
            when(dataPlaneClient.start(any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg));
            var dataPlaneInstance = createDataPlaneInstance();
            when(selectorService.select(any(), any())).thenReturn(ServiceResult.success(dataPlaneInstance));
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
        void shouldCallTerminateOnTheRightDataPlane() {
            var dataPlaneInstance = dataPlaneInstanceBuilder().id("dataPlaneId").build();
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("dataPlaneId")
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).terminate("transferProcessId");
            verify(dataPlaneClientFactory).createClient(dataPlaneInstance);
        }

        @Test
        void shouldFail_whenDataPlaneNotFound() {
            var transferProcess = transferProcessBuilder()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(dataPlaneClient.terminate(any())).thenReturn(StatusResult.success());
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = flowController.terminate(transferProcess);

            assertThat(result).isFailed().detail().contains("Failed to select the data plane for terminating the transfer process");
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
            verifyNoInteractions(dataPlaneClient, dataPlaneClientFactory, selectorService);
        }
    }

    @Nested
    class Suspend {

        @Test
        void shouldCallTerminate() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("dataPlaneId")
                    .build();
            when(dataPlaneClient.suspend(any())).thenReturn(StatusResult.success());
            var dataPlaneInstance = dataPlaneInstanceBuilder().id("dataPlaneId").build();
            when(dataPlaneClientFactory.createClient(any())).thenReturn(dataPlaneClient);
            when(selectorService.findById(any())).thenReturn(ServiceResult.success(dataPlaneInstance));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isSucceeded();
            verify(dataPlaneClient).suspend("transferProcessId");
            verify(dataPlaneClientFactory).createClient(dataPlaneInstance);
        }

        @Test
        void shouldFail_whenDataPlaneDoesNotExist() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId("invalid")
                    .build();
            when(selectorService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = flowController.suspend(transferProcess);

            assertThat(result).isFailed().detail().contains("Failed to select the data plane for suspending the transfer process");
            verifyNoInteractions(dataPlaneClient, dataPlaneClientFactory);
        }

        @Test
        void shouldFail_whenDataPlaneIdIsNull() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .id("transferProcessId")
                    .contentDataAddress(testDataAddress())
                    .dataPlaneId(null)
                    .build();

            var result = flowController.suspend(transferProcess);

            assertThat(result).isFailed().detail().contains("Failed to select the data plane for suspending the transfer process");
            verifyNoInteractions(dataPlaneClient, dataPlaneClientFactory, selectorService);
        }

    }

    @Nested
    class TransferTypes {

        @Test
        void transferTypes_shouldReturnTypesForSpecifiedAsset() {

            var assetNoResponse = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();
            when(transferTypeParser.parse(any()))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PUSH)))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PULL, "Response")))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PULL)));

            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH").allowedSourceType("TargetSrc").allowedDestType("TargetDest").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PULL").allowedTransferType("Custom-PULL-Response").allowedSourceType("TargetSrc").allowedDestType("AnotherTargetDest").build(),
                    dataPlaneInstanceBuilder().allowedSourceType("AnotherSrc").allowedDestType("ThisWontBeListed").build()
            )));

            var transferTypes = flowController.transferTypesFor(assetNoResponse);

            assertThat(transferTypes).containsExactly("Custom-PUSH", "Custom-PULL");
        }

        @Test
        void transferTypes_shouldFilterTypesForSpecifiedAssetWithResponseChannel() {
            when(transferTypeParser.parse(any()))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PUSH)))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PUSH)))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PULL, "Response")))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PULL)))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PUSH, "Response")));
            when(selectorService.getAll()).thenReturn(ServiceResult.success(List.of(
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH").allowedSourceType("TargetSrc").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Response-PUSH").allowedSourceType("TargetSrc").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PULL-Response").allowedTransferType("Custom-PULL").allowedSourceType("TargetSrc").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH-Response").allowedSourceType("AnotherSrc").build(),
                    dataPlaneInstanceBuilder().allowedSourceType("AnotherSrc").build()
            )));
            var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance()
                    .type("TargetSrc")
                    .property(EDC_DATA_ADDRESS_RESPONSE_CHANNEL, buildResponseChannel())
                    .build()).build();

            var transferTypes = flowController.transferTypesFor(asset);

            assertThat(transferTypes).containsExactly("Custom-PULL-Response");
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

}
