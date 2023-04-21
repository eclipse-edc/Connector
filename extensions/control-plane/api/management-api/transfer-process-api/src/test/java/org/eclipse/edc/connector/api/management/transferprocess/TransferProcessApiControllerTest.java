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

package org.eclipse.edc.connector.api.management.transferprocess;

import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.transferprocess.model.TerminateTransferDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
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
    private final TransferProcessService service = mock(TransferProcessService.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private TransferProcessApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new TransferProcessApiController(monitor, service, transformerRegistry);
    }

    @Test
    void queryAll() {
        var transferProcess = transferProcess();
        var dto = transferProcessDto(transferProcess);
        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        var querySpec = QuerySpecDto.Builder.newInstance().build();
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(transferProcess)));

        var transferProcesses = controller.queryAllTransferProcesses(querySpec);

        assertThat(transferProcesses).containsExactly(dto);
        verify(service).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(QuerySpecDto.class), eq(QuerySpec.class));
    }

    @Test
    void queryAll_filtersOutFailedTransforms() {
        var transferProcess = transferProcess();
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.failure("failure"));
        when(service.query(any())).thenReturn(ServiceResult.success(Stream.of(transferProcess)));

        var transferProcesses = controller.queryAllTransferProcesses(QuerySpecDto.Builder.newInstance().build());

        assertThat(transferProcesses).isEmpty();
    }

    @Test
    void queryAll_throwsExceptionIfQuerySpecTransformFails() {
        when(transformerRegistry.transform(isA(QuerySpecDto.class), eq(QuerySpec.class)))
                .thenReturn(Result.failure("Cannot transform"));

        assertThatThrownBy(() -> controller.queryAllTransferProcesses(QuerySpecDto.Builder.newInstance().build())).isInstanceOf(InvalidRequestException.class);
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

        assertThatThrownBy(() -> controller.deprovisionTransferProcess(transferProcess.getId())).isInstanceOf(ObjectConflictException.class);
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

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.success(transferProcess));

        controller.cancelTransferProcess(transferProcess.getId());
    }

    @Test
    void cancelTransfer_conflict() {
        var transferProcess = transferProcess();

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void cancelTransfer_NotFound() {
        var transferProcess = transferProcess();

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void terminateTransfer() {
        var transferProcess = transferProcess();

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.success(transferProcess));

        controller.terminateTransferProcess(transferProcess.getId(), TerminateTransferDto.Builder.newInstance().build());
    }

    @Test
    void terminateTransfer_conflict() {
        var transferProcess = transferProcess();

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.terminateTransferProcess(transferProcess.getId(), TerminateTransferDto.Builder.newInstance().build())).isInstanceOf(ObjectConflictException.class);
    }

    @Test
    void terminateTransfer_NotFound() {
        var transferProcess = transferProcess();

        when(service.terminate(eq(transferProcess.getId()), any())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.terminateTransferProcess(transferProcess.getId(), TerminateTransferDto.Builder.newInstance().build())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void initiateTransfer() {
        var transferReqDto = transferRequestDto();
        var processId = "processId";
        var tr = transferRequest(transferReqDto);
        var dr = tr.getDataRequest();

        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(TransferRequest.class))).thenReturn(Result.success(tr));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.success(processId));

        var result = controller.initiateTransfer(transferReqDto);

        var dataRequestCaptor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(service).initiateTransfer(dataRequestCaptor.capture());
        TransferRequest transferRequest = dataRequestCaptor.getValue();
        DataRequest dataRequest = transferRequest.getDataRequest();
        assertThat(dataRequest.getAssetId()).isEqualTo(dr.getAssetId());
        assertThat(dataRequest.getConnectorAddress()).isEqualTo(dr.getConnectorAddress());
        assertThat(dataRequest.getConnectorId()).isEqualTo(dr.getConnectorId());
        assertThat(dataRequest.getDataDestination()).isEqualTo(dr.getDataDestination());
        assertThat(dataRequest.getDestinationType()).isEqualTo(dr.getDataDestination().getType());
        assertThat(dataRequest.getContractId()).isEqualTo(dr.getContractId());
        assertThat(dataRequest.getProtocol()).isEqualTo(dr.getProtocol());
        assertThat(dataRequest.getProperties()).isEqualTo(dr.getProperties());
        assertThat(dataRequest.getTransferType()).isEqualTo(dr.getTransferType());
        assertThat(dataRequest.isManagedResources()).isEqualTo(dr.isManagedResources());

        assertThat(result.getId()).isEqualTo(processId);
        assertThat(result).isInstanceOf(IdResponseDto.class);
        assertThat(result.getId()).isNotEmpty();
        assertThat(result.getCreatedAt()).isNotEqualTo(0L);
    }

    @Test
    void initiateTransfer_failureTransformingRequest() {
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(TransferRequest.class))).thenReturn(Result.failure("failure"));

        assertThatThrownBy(() -> controller.initiateTransfer(transferRequestDto())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void initiateTransfer_failure() {
        var transferRequest = transferRequest();
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(TransferRequest.class))).thenReturn(Result.success(transferRequest));
        when(service.initiateTransfer(any())).thenReturn(ServiceResult.conflict("failure"));

        assertThatThrownBy(() -> controller.initiateTransfer(transferRequestDto())).isInstanceOf(EdcException.class);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidRequestParams.class)
    void initiateTransfer_invalidRequest(String connectorAddress, String contractId, String assetId, String protocol, DataAddress destination) {
        when(transformerRegistry.transform(isA(TransferRequestDto.class), eq(TransferRequest.class))).thenReturn(Result.failure("failure"));
        var rq = TransferRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .contractId(contractId)
                .protocol(protocol)
                .dataDestination(destination)
                .assetId(assetId)
                .build();
        assertThatThrownBy(() -> controller.initiateTransfer(rq)).isInstanceOfAny(InvalidRequestException.class);
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

    private TransferRequest transferRequest() {
        return TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest())
                .build();
    }

    private TransferRequest transferRequest(TransferRequestDto dto) {
        return TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest(dto))
                .build();
    }


    private TransferProcessDto transferProcessDto(TransferProcess transferProcess) {
        return TransferProcessDto.Builder.newInstance().id(transferProcess.getId()).build();
    }

    private TransferProcess transferProcess() {
        return transferProcess(UUID.randomUUID().toString());
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
