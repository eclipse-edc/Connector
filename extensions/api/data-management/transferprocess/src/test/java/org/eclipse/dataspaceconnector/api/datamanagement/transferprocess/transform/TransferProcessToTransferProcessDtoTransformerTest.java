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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.UNSAVED;
import static org.mockito.Mockito.when;

class TransferProcessToTransferProcessDtoTransformerTest {
    TransferProcessTransformerTestData data = new TransferProcessTransformerTestData();

    TransferProcessToTransferProcessDtoTransformer transformer = new TransferProcessToTransferProcessDtoTransformer();
    List<String> problems = new ArrayList<>();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcess.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(TransferProcessDto.class);
    }

    @Test
    void transform() {
        assertThatEntityTransformsToDto();
    }

    @Test
    void transform_whenInvalidState() {
        data.entity.state(invalidStateCode());
        data.dto.state(null);
        problems.add("Invalid value for TransferProcess.state");

        assertThatEntityTransformsToDto();
    }

    @Test
    void transform_whenMinimalData() {
        data.dto.state(UNSAVED.name());

        data.dataDestination = DataAddress.Builder.newInstance().type(data.dataDestinationType);
        data.dataRequest = DataRequest.Builder.newInstance()
                .dataDestination(data.dataDestination.build())
                .build();
        data.entity = TransferProcess.Builder.newInstance()
                .id(data.id)
                .type(data.type)
                .dataRequest(data.dataRequest);
        data.dto
                .dataDestination(Map.of("type", data.dataDestinationType))
                .errorDetail(null);

        assertThatEntityTransformsToDto();
    }

    void assertThatEntityTransformsToDto() {
        when(data.registry.transform(data.dataRequest, DataRequestDto.class)).thenReturn(Result.success(data.dataRequestDto));

        var result = transformer.transform(data.entity.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto.build());

        assertThat(data.context.getProblems()).containsExactlyElementsOf(problems);
    }

    private int invalidStateCode() {
        var stateCode = 0;
        while (TransferProcessStates.from(stateCode) != null) {
            stateCode++;
        }
        return stateCode;
    }
}