/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_ASSET_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_CONNECTOR_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_CONTRACT_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataRequestDtoTransformerTest {

    private final JsonObjectFromDataRequestDtoTransformer transformer = new JsonObjectFromDataRequestDtoTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(DataRequestDto.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var dto = DataRequestDto.Builder.newInstance()
                .id("id")
                .assetId("assetId")
                .connectorId("connectorId")
                .contractId("contractId")
                .build();

        var result = transformer.transform(dto, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("id");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_DATA_REQUEST_DTO_TYPE);
        assertThat(result.getString(EDC_DATA_REQUEST_DTO_ASSET_ID)).isEqualTo("assetId");
        assertThat(result.getString(EDC_DATA_REQUEST_DTO_CONNECTOR_ID)).isEqualTo("connectorId");
        assertThat(result.getString(EDC_DATA_REQUEST_DTO_CONTRACT_ID)).isEqualTo("contractId");
    }
}
