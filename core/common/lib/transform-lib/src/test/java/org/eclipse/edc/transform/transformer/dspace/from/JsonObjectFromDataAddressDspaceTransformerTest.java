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
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataAddressDspaceTransformerTest {

    private final JsonObjectFromDataAddressDspaceTransformer transformer = new JsonObjectFromDataAddressDspaceTransformer(
            Json.createBuilderFactory(Map.of()), JacksonJsonLd.createObjectMapper());
    private final TransformerContext context = mock();

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
}
