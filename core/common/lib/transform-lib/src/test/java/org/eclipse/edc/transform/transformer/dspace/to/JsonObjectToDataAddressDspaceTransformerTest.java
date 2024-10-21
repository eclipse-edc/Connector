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

package org.eclipse.edc.transform.transformer.dspace.to;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.transform.transformer.TestInput.getExpanded;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.DSPACE_DATAADDRESS_TYPE_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE_IRI;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectToDataAddressDspaceTransformerTest {
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final JsonObjectToDataAddressDspaceTransformer transformer = new JsonObjectToDataAddressDspaceTransformer();

    @Test
    void transform() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DSPACE_DATAADDRESS_TYPE_IRI)
                .add(ENDPOINT_TYPE_PROPERTY_IRI, "https://w3id.org/idsa/v4.1/HTTP")
                .add(ENDPOINT_PROPERTY_IRI, "http://example.com")
                .add(ENDPOINT_PROPERTIES_PROPERTY_IRI, jsonFactory.createArrayBuilder()
                        .add(property("authorization", "some-token"))
                        .add(property("authType", "bearer"))
                        .add(property("foo", "bar"))
                        .add(property("fizz", "buzz"))
                )
                .build();

        var dataAddress = transformer.transform(getExpanded(jsonObj), context);

        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getProperties())
                .containsEntry("authorization", "some-token")
                .containsEntry("authType", "bearer")
                .containsEntry("fizz", "buzz");
    }

    private JsonObjectBuilder property(String key, String value) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, ENDPOINT_PROPERTY_PROPERTY_TYPE_IRI)
                .add("name", key)
                .add("value", value);
    }

    private JsonArrayBuilder createContextBuilder() {
        return jsonFactory.createArrayBuilder()
                .add(jsonFactory.createObjectBuilder().add(VOCAB, DSPACE_SCHEMA))
                .add(jsonFactory.createObjectBuilder().add(DSPACE_PREFIX, DSPACE_SCHEMA));
    }

}
