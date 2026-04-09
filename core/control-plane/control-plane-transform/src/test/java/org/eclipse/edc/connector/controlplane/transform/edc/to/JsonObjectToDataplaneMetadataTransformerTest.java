/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataplaneMetadataTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TypeManager typeManager = mock();
    private TypeTransformerRegistry typeTransformerRegistry;

    @BeforeEach
    void setUp() {
        var transformer = new JsonObjectToDataplaneMetadataTransformer();
        typeTransformerRegistry = new TypeTransformerRegistryImpl();
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        typeTransformerRegistry.register(transformer);

        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void shouldTransformToDataplaneMetadata() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, jsonFactory.createObjectBuilder().add(VOCAB, EDC_NAMESPACE).add(EDC_PREFIX, EDC_NAMESPACE).build())
                .add(TYPE, EDC_DATAPLANE_METADATA_TYPE)
                .add(EDC_DATAPLANE_METADATA_LABELS, jsonFactory.createArrayBuilder().add("label"))
                .add(EDC_DATAPLANE_METADATA_PROPERTIES, jsonFactory.createObjectBuilder()
                        .add(VALUE, jsonFactory.createObjectBuilder().add("property", "value")))
                .build();

        var result = typeTransformerRegistry.transform(jsonObj, DataplaneMetadata.class);

        assertThat(result).isSucceeded().satisfies(metadata -> {
            assertThat(metadata.getLabels()).containsExactly("label");
            assertThat(metadata.getProperties()).hasEntrySatisfying("property", v -> {
                // the generic transformer wraps primitive values in their Java types -> expect String
                assertThat(v).isEqualTo("value");
            });
        });
    }

    @Test
    void transform_withInvalidProperties_shouldReportProblemAndReturnNull() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, jsonFactory.createObjectBuilder().add(VOCAB, EDC_NAMESPACE).add(EDC_PREFIX, EDC_NAMESPACE).build())
                .add(TYPE, EDC_DATAPLANE_METADATA_TYPE)
                // properties value is a plain string (invalid), not an object
                .add(EDC_DATAPLANE_METADATA_PROPERTIES, jsonFactory.createObjectBuilder().add(VALUE, "invalid-string"))
                .build();

        var result = typeTransformerRegistry.transform(jsonObj, DataplaneMetadata.class);

        assertThat(result).isFailed().detail().contains("Expected properties to be a JsonObject");
    }
}

