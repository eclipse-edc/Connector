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
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromDataAddressDtoTransformerTest {
    private static final String TEST_KEY = "region";
    private static final String TEST_VALUE = "europe";
    private final String type = "testType";
    private final String key = "testKey";
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private final JsonObjectFromDataAddressDtoTransformer transformer = new JsonObjectFromDataAddressDtoTransformer(jsonFactory);

    @Test
    void transform() {
        var message = DataAddressDto.Builder.newInstance()
                .properties(Map.of(
                        "type", type,
                        "keyName", key,
                        TEST_KEY, TEST_VALUE
                ))
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TEST_KEY).getString()).isEqualTo(TEST_VALUE);
        assertThat(result.getJsonString(DataAddress.SIMPLE_TYPE).getString()).isEqualTo(type);
        assertThat(result.getJsonString(DataAddress.SIMPLE_KEY_NAME).getString()).isEqualTo(key);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_withNamespace() {
        var schema = "https://some.custom.org/schema/";
        var message = DataAddressDto.Builder.newInstance()
                .properties(Map.of(
                        "type", type,
                        "keyName", key,
                        schema + TEST_KEY, TEST_VALUE
                ))
                .build();

        var result = transformer.transform(message, context);
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(schema + TEST_KEY).getString()).isEqualTo(TEST_VALUE);
        verify(context, never()).reportProblem(anyString());
    }
}
