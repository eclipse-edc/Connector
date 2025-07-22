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

package org.eclipse.edc.transform.transformer.dspace.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_IRI;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromDataAddressDspaceTransformerTest {

    private final TransformerContext context = mock();
    private final TypeManager typeManager = mock();
    private final JsonObjectFromDataAddressDspaceTransformer transformer = new JsonObjectFromDataAddressDspaceTransformer(
            Json.createBuilderFactory(Map.of()), typeManager, "context");


    @BeforeEach
    void setup() {
        when(typeManager.getMapper("context")).thenReturn(JacksonJsonLd.createObjectMapper());
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
        assertThat(jsonObject.getString(ENDPOINT_TYPE_PROPERTY_IRI)).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(jsonObject.get(ENDPOINT_PROPERTY_IRI)).isEqualTo(null);
        assertThat(jsonObject.getJsonArray(ENDPOINT_PROPERTIES_PROPERTY_IRI)).hasSize(3)
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI)).isEqualTo("authorization");
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI)).isEqualTo("secret-token");
                }).anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI)).isEqualTo("foo");
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI)).isEqualTo("bar");
                })
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI)).isEqualTo("endpoint");
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI)).isEqualTo("https://example.com");
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
        assertThat(jsonObject.getJsonArray(ENDPOINT_PROPERTIES_PROPERTY_IRI))
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI)).isEqualTo("foo");
                    assertThat(jv.asJsonObject().getJsonObject(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI))
                            .containsKey("complexObj");
                });
    }

    @Test
    void transform_withResponseChannelDataAddress() {
        var responseChannelDataAddress = DataAddress.Builder.newInstance()
                .type("responseType")
                .property("endpoint", "https://response.example.com")
                .build();
        var dataAddress = DataAddress.Builder.newInstance()
                .type("someType")
                .property("endpoint", "https://example.com")
                .property(DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL, responseChannelDataAddress)
                .build();

        when(context.transform(responseChannelDataAddress, JsonObject.class))
                .thenReturn(Json.createObjectBuilder()
                        .add(TYPE, "https://namespace/DataAddress")
                        .add(ENDPOINT_TYPE_PROPERTY_IRI, "responseType")
                        .add(ENDPOINT_PROPERTIES_PROPERTY_IRI, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI, "endpoint")
                                        .add(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI, "https://response.example.com")))
                        .build());

        var jsonObject = transformer.transform(dataAddress, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonArray(ENDPOINT_PROPERTIES_PROPERTY_IRI))
                .anySatisfy(jv -> {
                    assertThat(jv.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI))
                            .isEqualTo(DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL);
                    var responseChannelJson = jv.asJsonObject().getJsonObject(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI);
                    assertThat(responseChannelJson.getString(ENDPOINT_TYPE_PROPERTY_IRI))
                            .isEqualTo("responseType");
                    assertThat(responseChannelJson.getJsonArray(ENDPOINT_PROPERTIES_PROPERTY_IRI))
                            .hasSize(1)
                            .anySatisfy(prop -> {
                                assertThat(prop.asJsonObject().getString(ENDPOINT_PROPERTY_NAME_PROPERTY_IRI))
                                        .isEqualTo("endpoint");
                                assertThat(prop.asJsonObject().getString(ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI))
                                        .isEqualTo("https://response.example.com");
                            });
                });
    }

}
