/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *       Microsoft Corporation - support for id field
 *
 */

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransferRequestDtoToDataRequestTransformerTest {

    private final DtoTransformer<TransferRequestDto, DataRequest> transformer = new TransferRequestDtoToDataRequestTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferRequestDto.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(DataRequest.class);
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var transferReq = transferRequestDto()
                .id(UUID.randomUUID().toString())
                .build();
        var dataRequest = transformer.transform(transferReq, context);
        assertThat(dataRequest.getId()).isEqualTo(transferReq.getId());
        assertThat(dataRequest.getAssetId()).isEqualTo(transferReq.getAssetId());
        assertThat(dataRequest.getConnectorAddress()).isEqualTo(transferReq.getConnectorAddress());
        assertThat(dataRequest.getConnectorId()).isEqualTo(transferReq.getConnectorId());
        assertThat(dataRequest.getDataDestination()).isEqualTo(transferReq.getDataDestination());
        assertThat(dataRequest.getDestinationType()).isEqualTo(transferReq.getDataDestination().getType());
        assertThat(dataRequest.getContractId()).isEqualTo(transferReq.getContractId());
        assertThat(dataRequest.getProtocol()).isEqualTo(transferReq.getProtocol());
        assertThat(dataRequest.getProperties()).isEqualTo(transferReq.getProperties());
        assertThat(dataRequest.getTransferType()).isEqualTo(transferReq.getTransferType());
        assertThat(dataRequest.isManagedResources()).isEqualTo(transferReq.isManagedResources());
    }

    @Test
    void transform_whenIdIsNull_generatesId() {
        var context = mock(TransformerContext.class);
        var transferReq = transferRequestDto().build();
        var dataRequest = transformer.transform(transferReq, context);
        assertThat(transferReq.getId()).isBlank();
        assertThat(dataRequest.getId()).isNotBlank();
    }

    @NotNull
    private TransferRequestDto.Builder transferRequestDto() {
        return TransferRequestDto.Builder.newInstance()
                .connectorAddress("http://some.connector.address")
                .assetId(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .protocol("test-protocol")
                .dataDestination(DataAddress.Builder.newInstance().type("test-type").build())
                .connectorId(UUID.randomUUID().toString())
                .properties(Map.of("key1", "value1"));
    }

}
