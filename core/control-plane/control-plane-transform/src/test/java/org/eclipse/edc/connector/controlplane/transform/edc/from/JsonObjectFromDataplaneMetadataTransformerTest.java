/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.from;

import jakarta.json.Json;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.transform.TransformerContextImpl;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataplaneMetadataTransformerTest {

    private final JsonObjectFromDataplaneMetadataTransformer transformer = new JsonObjectFromDataplaneMetadataTransformer(
            Json.createBuilderFactory(emptyMap()),
            JacksonJsonLd::createObjectMapper
    );

    @Test
    void shouldTransformToJson() {
        var context = new TransformerContextImpl(mock());
        var dataplaneMetadata = DataplaneMetadata.Builder.newInstance().label("label").property("key", "value").build();

        var result = transformer.transform(dataplaneMetadata, context);

        assertThat(result.getJsonArray(EDC_DATAPLANE_METADATA_LABELS)).hasSize(1).first()
                .extracting(JsonString.class::cast).extracting(JsonString::getString).isEqualTo("label");
        assertThat(result.getJsonObject(EDC_DATAPLANE_METADATA_PROPERTIES)).hasSize(1)
                .satisfies(properties ->
                        assertThat(properties.get("key")).isInstanceOfSatisfying(JsonString.class, jsonString ->
                                assertThat(jsonString.getString()).isEqualTo("value")));
    }
}
