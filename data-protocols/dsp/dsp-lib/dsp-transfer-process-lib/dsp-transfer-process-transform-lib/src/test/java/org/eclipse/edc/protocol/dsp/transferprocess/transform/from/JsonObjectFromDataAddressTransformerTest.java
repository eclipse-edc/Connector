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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
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
        var innerDataAddress = DataAddress.Builder.newInstance()
                .property(EDC_DATA_ADDRESS_TYPE_PROPERTY, "type")
                .build();
        var message = DataAddress.Builder.newInstance()
                .type(type)
                .keyName(key)
                .property("string", "test")
                .property("integer", 123)
                .property("whatAboutDouble", 123.456)
                .property("nestedDataAddress", innerDataAddress)
                .property("nestedJsonObject", Map.of("key", Map.of("testKey", "testValue")))
                .property("jsonArray", List.of("string1", "string2"))
                .property("arrayOfObjects", List.of(
                        Map.of("key1", "value1"),
                        Map.of("key2", "value2")))
                .property("nestedJsonObjectWithArray", Map.of(
                        "key", List.of("value1", "value2"),
                        "anotherKey", List.of("value3", "value4")))
                .build();

        var expectedJson = jsonObject()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, type)
                .add(EDC_DATA_ADDRESS_KEY_NAME, key)
                .add("string", "test")
                .add("integer", 123)
                .add("whatAboutDouble", 123.456)
                .add("nestedDataAddress",
                        jsonObject()
                                .add("properties", jsonObject().add(EDC_DATA_ADDRESS_TYPE_PROPERTY, "type")))
                .add("nestedJsonObject",
                        jsonObject()
                                .add("key", jsonObject().add("testKey", "testValue")))
                .add("jsonArray",
                        jsonArray()
                                .add("string1").add("string2"))
                .add("arrayOfObjects",
                        jsonArray()
                                .add(jsonObject().add("key1", "value1"))
                                .add(jsonObject().add("key2", "value2")))
                .add("nestedJsonObjectWithArray",
                        jsonObject()
                                .add("key", jsonArray().add("value1").add("value2"))
                                .add("anotherKey", jsonArray().add("value3").add("value4")))
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).usingRecursiveAssertion().isEqualTo(expectedJson);
        verify(context, never()).reportProblem(anyString());
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

    private JsonObjectBuilder jsonObject() {
        return jsonFactory.createObjectBuilder();

    }

    private JsonArrayBuilder jsonArray() {
        return jsonFactory.createArrayBuilder();
    }
}
