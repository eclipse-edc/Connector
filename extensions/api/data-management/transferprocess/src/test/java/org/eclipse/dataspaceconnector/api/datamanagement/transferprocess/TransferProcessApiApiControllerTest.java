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
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessApiApiControllerTest {
    public static final int OFFSET = 5;
    public static final int LIMIT = 10;
    private final TransferProcessService service = mock(TransferProcessService.class);
    private final DtoTransformerRegistry transformerRegistry = mock(DtoTransformerRegistry.class);
    private TransferProcessApiController controller;
    private String filterExpression = "someField=value";
    private String someField = "someField";
    private static Faker faker = new Faker();


    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new TransferProcessApiController(monitor, service, transformerRegistry);
    }

    @Test
    void getAll() {

        TransferProcess transferProcess = transferProcess();
        var dto = transferProcessDto(transferProcess);

        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.success(dto));
        when(service.query(any())).thenReturn(List.of(transferProcess));

        assertThat(controller.getAllTransferProcesses(OFFSET, LIMIT, filterExpression, SortOrder.ASC, someField)).containsExactly(dto);
        assertQuerySpec(10, 5, SortOrder.ASC, someField, new Criterion(someField, "=", "value"));
    }

    @Test
    void getAll_getAll_filtersOutFailedTransforms() {
        TransferProcess transferProcess = transferProcess();

        when(transformerRegistry.transform(isA(TransferProcess.class), eq(TransferProcessDto.class))).thenReturn(Result.failure("failure"));
        when(service.query(any())).thenReturn(List.of(transferProcess));

        assertThat(controller.getAllTransferProcesses(OFFSET, LIMIT, filterExpression, SortOrder.ASC, someField)).isEmpty();
        assertQuerySpec(10, 5, SortOrder.ASC, someField, new Criterion(someField, "=", "value"));
    }

    @Test
    void getById() {
        String id = "tp-id";
        TransferProcess transferProcess = transferProcess(id);
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

        assertThat(controller.getTransferProcessState(id)).isEqualTo("PROVISIONING");
    }

    @Test
    void getStateById_notFound() {
        String id = "tp-id";

        when(service.getState(id)).thenReturn(null);

        assertThatThrownBy(() -> controller.getTransferProcessState(id)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void deprovision() {
        TransferProcess transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.success(transferProcess));

        controller.deprovisionTransferProcess(transferProcess.getId());
    }

    @Test
    void deprovision_conflict() {
        TransferProcess transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.deprovisionTransferProcess(transferProcess.getId())).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void deprovision_NotFound() {
        TransferProcess transferProcess = transferProcess();

        when(service.deprovision(transferProcess.getId())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.deprovisionTransferProcess(transferProcess.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void cancelTransfer() {
        TransferProcess transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.success(transferProcess));

        controller.cancelTransferProcess(transferProcess.getId());
    }

    @Test
    void cancelTransfer_conflict() {
        TransferProcess transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.conflict("conflict"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectExistsException.class);
    }

    @Test
    void cancelTransfer_NotFound() {
        TransferProcess transferProcess = transferProcess();

        when(service.cancel(transferProcess.getId())).thenReturn(ServiceResult.notFound("not found"));

        assertThatThrownBy(() -> controller.cancelTransferProcess(transferProcess.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("getInvalidRequestParams")
    void initiateTransfer_invalidRequest(String connectorAddress, String contractId, String assetId, String protocol, DataAddress destination) {
        var rq = TransferRequestDto.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .contractId(contractId)
                .protocol(protocol)
                .dataDestination(destination)
                .build();
        assertThatThrownBy(() -> controller.initiateTransfer(assetId, rq)).isInstanceOfAny(IllegalArgumentException.class);
    }


    // provides invalid values for a TransferRequestDto
    public static Stream<Arguments> getInvalidRequestParams() {
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

    private void assertQuerySpec(int limit, int offset, SortOrder sortOrder, String sortField, Criterion... criterions) {
        ArgumentCaptor<QuerySpec> captor = ArgumentCaptor.forClass(QuerySpec.class);
        verify(service).query(captor.capture());
        QuerySpec querySpec = captor.getValue();
        assertThat(querySpec.getFilterExpression()).containsExactly(criterions);
        assertThat(querySpec.getLimit()).isEqualTo(limit);
        assertThat(querySpec.getOffset()).isEqualTo(offset);
        assertThat(querySpec.getSortOrder()).isEqualTo(sortOrder);
        assertThat(querySpec.getSortField()).isEqualTo(sortField);
    }

    private TransferProcessDto transferProcessDto(TransferProcess transferProcess) {
        return TransferProcessDto.Builder.newInstance().id(transferProcess.getId()).build();
    }

    private TransferProcess transferProcess() {
        return transferProcess(faker.lorem().word());
    }

    private TransferProcess transferProcess(String id) {
        return TransferProcess.Builder.newInstance().id(id).build();
    }
}
