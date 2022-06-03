/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessApiControllerTest {
    private static final Faker FAKER = new Faker();
    private final TransferProcessService service = mock(TransferProcessService.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private TransferProcessApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new TransferProcessApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getAll() {
        var transferProcess = transferProcess();
        var dto = transferProcessDto(transferProcess);
        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();
        when(service.query(any())).thenReturn(List.of(transferProcess));

        var transferProcesses = controller.getAllTransferProcesses(querySpec);

        assertThat(transferProcesses).containsExactly(dto);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void getAll_filtersOutFailedTransforms() {
        var transferProcess = transferProcess();
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.failure("failure"));
        when(service.query(any())).thenReturn(List.of(transferProcess));

        var transferProcesses = controller.getAllTransferProcesses(QuerySpecDto.Builder.newInstance().build());

        assertThat(transferProcesses).isEmpty();
    }

    @Test
    void getAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.getAllTransferProcesses(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById() {
        String id = "tp-id";
        var transferProcess = transferProcess(id);
        TransferProcessDto dto = transferProcessDto(transferProcess);

        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(service.findById(id)).thenReturn(transferProcess);

        assertThat(controller.getTransferProcess(id)).isEqualTo(dto);
    }

    @Test
    void getById_notFound() {
        String id = "tp-id";
        when(service.findById(id)).thenReturn(null);

        assertThatThrownBy(() -> controller.getTransferProcess(id)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void getStateById() {
        String id = "tp-id";

        when(service.getState(id)).thenReturn("PROVISIONING");

        assertThat(controller.getTransferProcessState(id).getState()).isEqualTo("PROVISIONING");
    }

    @Test
    void getStateById_notFound() {
        String id = "tp-id";

        when(service.getState(id)).thenReturn(null);

        assertThatThrownBy(() -> controller.getTransferProcessState(id)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deprovision() {
        var transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.success(transferProcess));

        controller.deprovisionTransferProcess(transferProcess.getId());
    }

    @Test
    void deprovision_conflict() {
        var transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.deprovisionTransferProcess(transferProcess.getId())).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void deprovision_NotFound() {
        var transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.deprovisionTransferProcess(transferProcess.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void cancelTransfer() {
        var transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.success(transferProcess));

        controller.cancelTransferProcess(transferProcess.getId());
    }

    @Test
    void cancelTransfer_conflict() {
        var transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void cancelTransfer_NotFound() {
        var transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void initiateTransfer() {
        var transferReq = transferRequestDto();
        String processId = "processId";
        DataRequest request = dataRequest(transferReq);
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(DataRequest.class))).thenReturn(Result.success(request));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.success(processId));

        String result = controller.initiateTransfer(transferReq).getId();

        var dataRequestCaptor = ArgumentCaptor.forClass(DataRequest.class);
        verify(service).initiateTransfer(dataRequestCaptor.capture());
        DataRequest dataRequest = dataRequestCaptor.getValue();
        assertThat(dataRequest.getAssetId()).isEqualTo(request.getAssetId());
        assertThat(dataRequest.getConnectorAddress()).isEqualTo(request.getConnectorAddress());
        assertThat(dataRequest.getConnectorId()).isEqualTo(request.getConnectorId());
        assertThat(dataRequest.getDataDestination()).isEqualTo(request.getDataDestination());
        assertThat(dataRequest.getDestinationType()).isEqualTo(request.getDataDestination().getType());
        assertThat(dataRequest.getContractId()).isEqualTo(request.getContractId());
        assertThat(dataRequest.getProtocol()).isEqualTo(request.getProtocol());
        assertThat(dataRequest.getProperties()).isEqualTo(request.getProperties());
        assertThat(dataRequest.getTransferType()).isEqualTo(request.getTransferType());
        assertThat(dataRequest.isManagedResources()).isEqualTo(request.isManagedResources());

        assertThat(result).isEqualTo(processId);
    }

    @Test
    void initiateTransfer_failureTransformingRequest() {
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(DataRequest.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.initiateTransfer(transferRequestDto())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initiateTransfer_failure() {
        var dataRequest = dataRequest();
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(DataRequest.class))).thenReturn(Result.success(dataRequest));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.conflict("failure"));

        assertThatThrownBy(() -> controller.initiateTransfer(transferRequestDto())).isInstanceOf(EdcException.class);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidRequestParams.class)
    void initiateTransfer_invalidRequest(String connectorAddress, String contractId, String assetId, String protocol, DataAddress destination) {
        var rq = TransferRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .contractId(contractId)
                .protocol(protocol)
                .dataDestination(destination)
                .assetId(assetId)
                .build();
        assertThatThrownBy(() -> controller.initiateTransfer(rq)).isInstanceOfAny(IllegalArgumentException.class);
    }

    private TransferRequestDto transferRequestDto() {
        return TransferRequestDto.Builder.newInstance()
                .assetId("assetId")
                .connectorAddress("http://some-contract")
                .contractId("some-contract")
                .protocol("test-asset")
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .connectorId("connectorId")
                .properties(Map.of("prop", "value"))
                .build();
    }

    private DataRequest dataRequest() {
        return DataRequest.Builder.newInstance()
                .dataDestination(DataAddress.Builder.newInstance().type("dataaddress-type").build())
                .build();
    }

    private TransferProcessDto transferProcessDto(TransferProcess transferProcess) {
        return TransferProcessDto.Builder.newInstance().id(transferProcess.getId()).build();
    }

    private TransferProcess transferProcess() {
        return transferProcess(FAKER.lorem().word());
    }

    private TransferProcess transferProcess(String id) {
        return TransferProcess.Builder.newInstance().id(id).build();
    }

    private DataRequest dataRequest(TransferRequestDto dto) {
        return DataRequest.Builder.newInstance()
                .assetId(dto.getAssetId())
                .connectorId(dto.getConnectorId())
                .dataDestination(dto.getDataDestination())
                .connectorAddress(dto.getConnectorAddress())
                .contractId(dto.getContractId())
                .transferType(dto.getTransferType())
                .destinationType(dto.getDataDestination().getType())
                .properties(dto.getProperties())
                .managedResources(dto.isManagedResources())
                .protocol(dto.getProtocol())
                .dataDestination(dto.getDataDestination())
                .build();
    }

    private static class InvalidRequestParams implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(null, "some-contract", "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("", "some-contract", "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("  ", "some-contract", "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", null, "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "", "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "  ", "test-asset", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "test-asset", null, DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "test-asset", "", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "test-asset", "  ", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "test-asset", "ids-multipart", null),
                    Arguments.of("http://someurl", "some-contract", null, "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build()),
                    Arguments.of("http://someurl", "some-contract", "  ", "ids-multipart", DataAddress.Builder.newInstance().type("test-type").build())
            );
        }
    }
}
