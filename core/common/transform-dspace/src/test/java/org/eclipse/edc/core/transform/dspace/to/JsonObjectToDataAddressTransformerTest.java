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

package org.eclipse.edc.core.transform.dspace.to;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.core.transform.dspace.TestFunctions;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.core.transform.dspace.DspaceDataAddressSerialization.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.core.transform.dspace.DspaceDataAddressSerialization.ENDPOINT_PROPERTIES_PROPERTY;
import static org.eclipse.edc.core.transform.dspace.DspaceDataAddressSerialization.ENDPOINT_PROPERTY;
import static org.eclipse.edc.core.transform.dspace.DspaceDataAddressSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE;
import static org.eclipse.edc.core.transform.dspace.DspaceDataAddressSerialization.ENDPOINT_TYPE_PROPERTY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.mockito.Mockito.mock;

class JsonObjectToDataAddressTransformerTest {
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final JsonObjectToDataAddressTransformer transformer = new JsonObjectToDataAddressTransformer();

    @Test
    void transform() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DSPACE_DATAADDRESS_TYPE)
                .add(ENDPOINT_TYPE_PROPERTY, "https://w3id.org/idsa/v4.1/HTTP")
                .add(ENDPOINT_PROPERTY, "http://example.com")
                .add(ENDPOINT_PROPERTIES_PROPERTY, jsonFactory.createArrayBuilder()
                        .add(property("authorization", "some-token"))
                        .add(property("authType", "bearer"))
                        .add(property("foo", "bar"))
                        .add(property("fizz", "buzz"))
                )
                .build();

        var expanded = TestFunctions.getExpanded(jsonObj);

        var dataAddress = transformer.transform(expanded, context);
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getProperties())
                .containsEntry("authorization", "some-token")
                .containsEntry("authType", "bearer")
                .containsEntry("fizz", "buzz");
    }

    @Test
    void transform_withIllegalProperty() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DSPACE_DATAADDRESS_TYPE)
                .add(ENDPOINT_TYPE_PROPERTY, "https://w3id.org/idsa/v4.1/HTTP")
                .add(ENDPOINT_PROPERTY, "http://example.com")
                .add(ENDPOINT_PROPERTIES_PROPERTY, jsonFactory.createArrayBuilder()
                        .add(property("fizz", "buzz"))
                )
                .add("rogueProperty", 42L)
                .build();

        var expanded = TestFunctions.getExpanded(jsonObj);

        assertThatThrownBy(() -> transformer.transform(expanded, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unexpected value: %srogueProperty".formatted(DSPACE_SCHEMA));
    }

    private JsonObjectBuilder property(String key, String value) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, ENDPOINT_PROPERTY_PROPERTY_TYPE)
                .add("name", key)
                .add("value", value);
    }

    private JsonArrayBuilder createContextBuilder() {
        return jsonFactory.createArrayBuilder()
                .add(jsonFactory.createObjectBuilder().add(VOCAB, DSPACE_SCHEMA))
                .add(jsonFactory.createObjectBuilder().add(DSPACE_PREFIX, DSPACE_SCHEMA));
    }

}
