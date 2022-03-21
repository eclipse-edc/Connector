/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 *    Microsoft Corporation - Added initiate-transfer endpoint tests
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class AssetApiControllerTest {

    private AssetApiController assetController;

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

    @BeforeEach
    void setUp() {
        var monitor = mock(Monitor.class);
        assetController = new AssetApiController(monitor);
    }

    @Test
    void createAsset() {
        //Todo: implement
    }

    @Test
    void createAsset_alreadyExists() {
        //Todo: implement
    }

    @Test
    void getAllAssets() {
        //Todo: implement
    }

    @Test
    void getAssetById() {
        //Todo: implement
    }

    @Test
    void getAssetById_notExists() {
        //Todo: implement
    }

    @Test
    void deleteAsset() {
        //Todo: implement
    }

    @Test
    void deleteAsset_notExists() {
        //Todo: implement
    }

    @Test
    void initiateTransfer() {
        //Todo: implement
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
        assertThatThrownBy(() -> assetController.initiateTransfer(assetId, rq)).isInstanceOfAny(IllegalArgumentException.class);
    }

}
