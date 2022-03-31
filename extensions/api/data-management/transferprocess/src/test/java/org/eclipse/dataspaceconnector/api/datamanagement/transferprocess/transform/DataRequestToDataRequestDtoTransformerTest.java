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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataRequestToDataRequestDtoTransformerTest {
    DataRequestTransformerTestData data = new DataRequestTransformerTestData();

    DataRequestToDataRequestDtoTransformer transformer = new DataRequestToDataRequestDtoTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(DataRequest.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(DataRequestDto.class);
    }

    @Test
    void transform() {
        var result = transformer.transform(data.entity, data.context);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto);
    }
}