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
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.spi.Namespaces;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL;
import static org.eclipse.edc.transform.transformer.TestInput.getExpanded;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.DSPACE_DATAADDRESS_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataAddressDspaceTransformerTest {
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final JsonObjectToDataAddressDspaceTransformer transformer = new JsonObjectToDataAddressDspaceTransformer(DSP_NAMESPACE_V_2025_1);

    @Test
    void transform() {
        var nestedResponseChannel = responseChannelAddress();
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, jsonFactory.createArrayBuilder().add(Namespaces.DSPACE_CONTEXT_2025_1))
                .add(TYPE, DSPACE_DATAADDRESS_TYPE_TERM)
                .add(ENDPOINT_TYPE_PROPERTY_TERM, "https://w3id.org/idsa/v4.1/HTTP")
                .add(ENDPOINT_PROPERTY_TERM, "http://example.com")
                .add(ENDPOINT_PROPERTIES_PROPERTY_TERM, jsonFactory.createArrayBuilder()
                        .add(property("authorization", "some-token"))
                        .add(property("authType", "bearer"))
                        .add(property("foo", "bar"))
                        .add(property("fizz", "buzz"))
                        .add(propertyWith(nestedResponseChannel))
                )
                .build();

        when(context.transform(any(), eq(DataAddress.class)))
                .thenReturn(DataAddress.Builder.newInstance()
                        .type("SomeType")
                        .property("john", "doe")
                        .property("internal", "prop")
                        .build());

        var dataAddress = transformer.transform(getExpanded(jsonObj), context);

        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getProperties())
                .containsEntry("authorization", "some-token")
                .containsEntry("authType", "bearer")
                .containsEntry("fizz", "buzz");
        assertThat(dataAddress.getResponseChannel()).isNotNull();
        assertThat(dataAddress.getResponseChannel().getProperties())
                .containsEntry("john", "doe")
                .containsEntry("internal", "prop");
    }

    private JsonObjectBuilder property(String key, String value) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM)
                .add("name", key)
                .add("value", value);
    }

    private JsonObjectBuilder propertyWith(JsonObjectBuilder builder) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM)
                .add("name", EDC_DATA_ADDRESS_RESPONSE_CHANNEL)
                .add("value", builder);
    }

    private JsonObjectBuilder responseChannelAddress() {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, DSPACE_DATAADDRESS_TYPE_TERM)
                .add(ENDPOINT_TYPE_PROPERTY_TERM, "SomeType")
                .add(ENDPOINT_PROPERTIES_PROPERTY_TERM, jsonFactory.createArrayBuilder()
                        .add(property("john", "doe"))
                        .add(property("internal", "prop")));
    }

}
