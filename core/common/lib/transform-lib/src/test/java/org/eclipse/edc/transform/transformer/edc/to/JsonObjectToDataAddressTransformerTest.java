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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class JsonObjectToDataAddressTransformerTest {
    private static final int CUSTOM_PAYLOAD_AGE = 34;
    private static final String CUSTOM_PAYLOAD_NAME = "max";
    private static final String TEST_TYPE = "test-type";
    private static final String TEST_KEY_NAME = "test-key-name";
    private final TypeManager typeManager = mock();

    private final JsonValueToGenericTypeTransformer objectTransformer = new JsonValueToGenericTypeTransformer(typeManager, "test");
    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObjectToDataAddressTransformer transformer;
    private TransformerContext transformerContext;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataAddressTransformer();
        transformerContext = mock(TransformerContext.class);
        when(transformerContext.transform(isA(JsonObject.class), eq(Object.class)))
                .thenAnswer(i -> objectTransformer.transform(i.getArgument(0), transformerContext));
        when(transformerContext.problem()).thenReturn(new ProblemBuilder(transformerContext));
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var json = expand(createDataAddress().build());

        var dataAddress = transformer.transform(json, transformerContext);
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo(TEST_TYPE);
        assertThat(dataAddress.getKeyName()).isEqualTo(TEST_KEY_NAME);
    }

    @Test
    void transform_withCustomProps() {
        var json = createDataAddress()
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add("my-test-prop", "some-test-value")
                        .build())
                .build();

        var dataAddress = transformer.transform(expand(json), transformerContext);

        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo(TEST_TYPE);
        assertThat(dataAddress.getStringProperty(EDC_NAMESPACE + "my-test-prop")).isEqualTo("some-test-value");
        assertThat(dataAddress.getProperties()).hasSize(3);
    }

    @Test
    void transform_withComplexCustomProps() {
        var json = createDataAddress()
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add("payload", createPayloadBuilder()
                                .add("sabesxopaulovem", createPayloadBuilder()
                                        .build())
                                .build())
                        .add("array", createArrayBuilder().add("string1").add("string2").build())
                        .build())
                .build();

        var dataAddress = transformer.transform(expand(json), transformerContext);

        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getProperties()).hasSize(4);

        assertThat(dataAddress.getProperty(EDC_NAMESPACE + "payload"))
                .isNotNull()
                .asInstanceOf(MAP)
                .hasSize(4)
                .containsKeys(EDC_NAMESPACE + "name", EDC_NAMESPACE + "age")
                .containsValues(List.of(Map.of(VALUE, CUSTOM_PAYLOAD_NAME)), List.of(Map.of(VALUE, CUSTOM_PAYLOAD_AGE)))
                .hasEntrySatisfying(EDC_NAMESPACE + "sabesxopaulovem",
                        v -> assertThat(v).asInstanceOf(LIST).first().asInstanceOf(MAP)
                                .hasSize(3)
                                .containsEntry(EDC_NAMESPACE + "name", List.of(Map.of(VALUE, CUSTOM_PAYLOAD_NAME)))
                                .containsEntry(EDC_NAMESPACE + "age", List.of(Map.of(VALUE, CUSTOM_PAYLOAD_AGE))));

        assertThat(dataAddress.getProperty(EDC_NAMESPACE + "array"))
                .isNotNull()
                .isEqualTo(List.of("string1", "string2"));

        verify(transformerContext, never()).reportProblem(any());
    }

    private JsonObjectBuilder createDataAddress() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, TEST_TYPE)
                .add(DataAddress.EDC_DATA_ADDRESS_KEY_NAME, TEST_KEY_NAME);
    }

    private JsonObjectBuilder createContextBuilder() {
        return createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private JsonObjectBuilder createPayloadBuilder() {
        return createObjectBuilder()
                .add(TYPE, "customPayload")
                .add("name", CUSTOM_PAYLOAD_NAME)
                .add("age", CUSTOM_PAYLOAD_AGE);
    }

    private JsonObject expand(JsonObject jsonObject) {
        return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }
}
