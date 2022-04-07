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

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TransferProcessApiApiControllerTest {
    private TransferProcessApiController controller;

    @BeforeEach
    void setup() {
        var monitor = mock(Monitor.class);
        controller = new TransferProcessApiController(monitor);
    }

    @Test
    void getAll_paging_noFilter() {
        //todo: implement
    }

    @Test
    void getAll_paging_pageSizeTooLarge() {
        //todo: implement
    }

    @Test
    void getAll_paging_offsetOutOfBounds() {
        //todo: implement
    }

    @ParameterizedTest
    @ValueSource(strings = { "id=id1", "id = id1", "id =id1", "id= id1" })
    void getAll_paging_withValidFilter(String filter) {
        //todo: implement
    }


    @ParameterizedTest
    @ValueSource(strings = { "id > id1", "id < id1", "id like id1", "id inside id1" })
    void getAll_paging_filterWithInvalidOperator(String filter) {
        //todo: implement
    }

    @ParameterizedTest
    @ValueSource(strings = { "id>id1", "id<id1" })
    void getAll_paging_withInvalidFilter(String filter) {
        //todo: implement
    }

    @Test
    void getAll_paging_invalidParams() {
        //todo: implement
    }

    @Test
    void getAll_noPaging() {
        //todo: implement
    }

    @Test
    void getTransferProcess_found() {
        //todo: implement
    }

    @Test
    void getTransferProcess_notFound() {
        assertThat(controller.getTransferProcess("not-exist")).isNull();
    }

    @Test
    void cancelTransferProcess_success() {
        //todo: implement

    }


    @Test
    void cancelContractDefinition_notFound() {
        //todo: implement
    }

    @Test
    void cancelContractDefinition_alreadyCancelled() {
        //todo: implement
    }

    @Test
    void cancelContractDefinition_notPossible() {
        // not possible when in state CANCELLED, ERROR, DEPROVISIONING, DEPROVISIONING_REQ, DEPROVISIONED, COMPLETE or ENDED
        //todo: implement
    }

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

}
