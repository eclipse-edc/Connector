/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromDataAddressTransformerTest {
    private final String type = "testType";

    private final String key = "testKey";

    private final String propertyKey = "region";

    private final String propertyValue = "europe";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromDataAddressTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataAddressTransformer(jsonFactory);
    }

    @Test
    void transformTransferCompletionMessage() {
        var message = DataAddress.Builder.newInstance()
                        .type(type)
                        .keyName(key)
                        .property(propertyKey, propertyValue)
                        .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(propertyKey).getString()).isEqualTo(propertyValue);
        assertThat(result.getJsonString("type").getString()).isEqualTo(type);
        assertThat(result.getJsonString("keyName").getString()).isEqualTo(key);

        verify(context, never()).reportProblem(anyString());
    }
}
