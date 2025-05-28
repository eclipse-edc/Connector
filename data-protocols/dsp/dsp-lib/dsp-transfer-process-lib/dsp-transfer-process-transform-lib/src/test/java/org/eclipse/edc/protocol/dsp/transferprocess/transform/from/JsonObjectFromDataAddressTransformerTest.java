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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_KEY_NAME;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class JsonObjectFromDataAddressTransformerTest {
    private static final String TEST_KEY = "region";
    private static final String TEST_VALUE = "europe";
    private final String type = "testType";
    private final String key = "testKey";
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final TypeManager typeManager = mock();

    private JsonObjectFromDataAddressTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataAddressTransformer(jsonFactory, typeManager, "test");
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var message = DataAddress.Builder.newInstance()
                .type(type)
                .keyName(key)
                .property(TEST_KEY, TEST_VALUE)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TEST_KEY).getString()).isEqualTo(TEST_VALUE);
        assertThat(result.getJsonString(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY).getString()).isEqualTo(type);
        assertThat(result.getJsonString(DataAddress.EDC_DATA_ADDRESS_KEY_NAME).getString()).isEqualTo(key);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_withCustomComplexProperty() {
        var innerDataAddress = DataAddress.Builder.newInstance().property(EDC_DATA_ADDRESS_TYPE_PROPERTY, "type").build();
        var message = DataAddress.Builder.newInstance()
                .type(type)
                .keyName(key)
                .property("complexJsonObject", innerDataAddress)
                .property("complexJsonArray", List.of("string1", "string2"))
                .build();

        var expectedJson = jsonFactory.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, type)
                .add(EDC_DATA_ADDRESS_KEY_NAME, key)
                .add("complexJsonObject", jsonFactory.createObjectBuilder()
                        .add("properties", jsonFactory.createObjectBuilder()
                                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, "type")))
                .add("complexJsonArray", jsonFactory.createArrayBuilder()
                        .add("string1")
                        .add("string2"))
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).usingRecursiveAssertion().isEqualTo(expectedJson);


    }

    @Test
    void transform_withNamespace() {
        var schema = "https://some.custom.org/schema/";
        var message = DataAddress.Builder.newInstance()
                .type(type)
                .keyName(key)
                .property(schema + TEST_KEY, TEST_VALUE)
                .build();

        var result = transformer.transform(message, context);
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(schema + TEST_KEY).getString()).isEqualTo(TEST_VALUE);
        verify(context, never()).reportProblem(anyString());
    }
}
