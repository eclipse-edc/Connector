/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform.transformer.dspace.v2024.from;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromDataAddressDspace2024TransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final TypeManager typeManager = mock();
    private final JsonObjectFromDataAddressDspace2024Transformer transformer = new JsonObjectFromDataAddressDspace2024Transformer(
            Json.createBuilderFactory(Map.of()), typeManager, "test", DSP_NAMESPACE);
    private final TransformerContext context = mock();

    @BeforeEach
    void setup() {
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("https://w3id.org/idsa/v4.1/HTTP")
                .property("endpoint", "https://example.com")
                .property("authorization", "secret-token")
                .property("foo", "bar")
                .build();

        var jsonObject = transformer.transform(dataAddress, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonObject(DSP_NAMESPACE.toIri(ENDPOINT_TYPE_PROPERTY_TERM)).getString(ID)).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(jsonObject.get(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_TERM))).isEqualTo(null);
        assertThat(jsonObject.getJsonArray(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTIES_PROPERTY_TERM))).hasSize(3)
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM))).isEqualTo("authorization");
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM))).isEqualTo("secret-token");
                }).anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM))).isEqualTo("foo");
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM))).isEqualTo("bar");
                })
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM))).isEqualTo("endpoint");
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM))).isEqualTo("https://example.com");
                });
    }

    @Test
    void transform_withComplexProperty() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("https://w3id.org/idsa/v4.1/HTTP")
                .property("endpoint", "https://example.com")
                .property("authorization", "secret-token")
                .property("foo", Map.of("complexObj", Map.of("key1", "value1", "key2", Map.of("key3", "value3"))))
                .build();

        var jsonObject = transformer.transform(dataAddress, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonArray(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTIES_PROPERTY_TERM)))
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM))).isEqualTo("foo");
                    assertThat(jv.asJsonObject().getJsonObject(DSP_NAMESPACE.toIri(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM)))
                            .containsKey("complexObj");
                });
    }
}
