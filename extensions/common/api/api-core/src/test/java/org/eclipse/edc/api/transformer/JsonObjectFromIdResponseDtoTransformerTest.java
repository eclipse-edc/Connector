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

package org.eclipse.edc.api.transformer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_CREATED_AT;
import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromIdResponseDtoTransformerTest {

    private final JsonObjectFromIdResponseDtoTransformer transformer = new JsonObjectFromIdResponseDtoTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(IdResponseDto.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var input = IdResponseDto.Builder.newInstance()
                .id("id")
                .createdAt(1234)
                .build();

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("id");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_ID_RESPONSE_DTO_TYPE);
        assertThat(result.getInt(EDC_ID_RESPONSE_DTO_CREATED_AT)).isEqualTo(1234);
    }

}
