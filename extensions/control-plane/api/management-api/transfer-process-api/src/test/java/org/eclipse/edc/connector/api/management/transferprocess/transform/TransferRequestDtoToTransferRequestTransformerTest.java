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
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferRequestDtoToTransferRequestTransformerTest {

    private final DtoTransformer<TransferRequestDto, TransferRequest> transformer = new TransferRequestDtoToTransferRequestTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferRequestDto.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(TransferRequest.class);
    }

    @Test
    void transform() {
        String destinationType = "test-type";
        var context = mock(TransformerContext.class);
        var transferReq = transferRequestDto()
                .id(UUID.randomUUID().toString())
                .build();
        when(context.transform(isA(TransferRequestDto.class), eq(DataRequest.class))).thenReturn(dataRequest(destinationType));

        var transferRequest = transformer.transform(transferReq, context);
        assertThat(transferRequest.getDataRequest())
                .isNotNull()
                .extracting(DataRequest::getDestinationType)
                .isEqualTo(destinationType);
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


    private DataRequest dataRequest(String destinationType) {
        return DataRequest.Builder.newInstance().destinationType(destinationType).build();
    }

}
