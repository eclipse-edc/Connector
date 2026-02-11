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

package org.eclipse.edc.transform.transformer.edc.from;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_TRANSFER_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_STATE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DESTINATION_PROVISION_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.LAST_ACTIVE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.PROPERTIES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromDataPlaneInstanceTransformerTest {

    private final TransformerContext context = mock();
    private final TypeManager typeManager = mock();
    private JsonObjectFromDataPlaneInstanceTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataPlaneInstanceTransformer(Json.createBuilderFactory(Map.of()), typeManager, "test");
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var dpi = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://foo.bar")
                .allowedSourceType("test-source-type")
                .allowedTransferType("test-transfer-type")
                .destinationProvisionTypes(Set.of("test-destination-type"))
                .lastActive(15)
                .state(REGISTERED.code())
                .property("foo", "bar")
                .build();

        var jsonObject = transformer.transform(dpi, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getString(ID)).isEqualTo("test-id");
        assertThat(jsonObject.getString(URL)).isEqualTo("http://foo.bar");
        assertThat(jsonObject.get(ALLOWED_SOURCE_TYPES)).isInstanceOfSatisfying(JsonArray.class, singleItemEqualTo("test-source-type"));
        assertThat(jsonObject.get(ALLOWED_TRANSFER_TYPES)).isInstanceOfSatisfying(JsonArray.class, singleItemEqualTo("test-transfer-type"));
        assertThat(jsonObject.get(DESTINATION_PROVISION_TYPES)).isInstanceOfSatisfying(JsonArray.class, singleItemEqualTo("test-destination-type"));
        assertThat(jsonObject.getString(DATAPLANE_INSTANCE_STATE)).isEqualTo(REGISTERED.name());
        assertThat(jsonObject.getJsonNumber(LAST_ACTIVE).intValue()).isEqualTo(15);
        assertThat(jsonObject.getJsonObject(PROPERTIES).getJsonString("foo").getString()).isEqualTo("bar");
    }

    private Consumer<JsonArray> singleItemEqualTo(String value) {
        return list -> assertThat(list).hasSize(1).first()
                .extracting(JsonString.class::cast)
                .extracting(JsonString::getString)
                .isEqualTo(value);
    }

}
