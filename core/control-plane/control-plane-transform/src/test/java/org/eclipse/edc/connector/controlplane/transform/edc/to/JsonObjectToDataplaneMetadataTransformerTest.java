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
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata.EDC_DATAPLANE_METADATA_SIMPLE_TYPE;
import static org.eclipse.edc.connector.controlplane.transform.TestInput.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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

    @ParameterizedTest
    @ArgumentsSource(Contexts.class)
    void shouldTransformToDataplaneMetadata(JsonValue context) {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, context)
                .add(TYPE, EDC_DATAPLANE_METADATA_SIMPLE_TYPE)
                .add("labels", jsonFactory.createArrayBuilder().add("label").add("label2"))
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("property", "value")
                )
                .build();

        var result = typeTransformerRegistry.transform(getExpanded(jsonObj), DataplaneMetadata.class);

        assertThat(result).isSucceeded().satisfies(metadata -> {
            assertThat(metadata.getLabels()).containsExactly("label", "label2");
            assertThat(metadata.getProperties()).hasSize(1).hasEntrySatisfying(prefix(context) + "property", v -> {
                assertThat(v).isEqualTo("value");
            });
        });
    }

    @ParameterizedTest
    @ArgumentsSource(Contexts.class)
    void shouldNotFail_whenPropertiesIsEmpty(JsonValue context) {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, context)
                .add(TYPE, EDC_DATAPLANE_METADATA_SIMPLE_TYPE)
                .add(EDC_DATAPLANE_METADATA_PROPERTIES, jsonFactory.createArrayBuilder())
                .build();

        var result = typeTransformerRegistry.transform(getExpanded(jsonObj), DataplaneMetadata.class);

        assertThat(result).isSucceeded().satisfies(metadata -> {
            assertThat(metadata.getProperties()).isEmpty();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(Contexts.class)
    void shouldFail_whenLabelIsNotString(JsonValue context) {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, context)
                .add(TYPE, EDC_DATAPLANE_METADATA_SIMPLE_TYPE)
                .add(EDC_DATAPLANE_METADATA_LABELS, jsonFactory.createArrayBuilder().add(1))
                .build();

        var result = typeTransformerRegistry.transform(getExpanded(jsonObj), DataplaneMetadata.class);

        assertThat(result).isFailed();
    }

    private static String prefix(JsonValue context) {
        if (context instanceof JsonObject jsonObject && jsonObject.containsKey(VOCAB)) {
            return jsonObject.getJsonString(VOCAB).getString();
        }
        return "";
    }

    private static class Contexts implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            var jsonFactory = Json.createBuilderFactory(Map.of());
            return Stream.of(
                    arguments(jsonFactory.createObjectBuilder().add(VOCAB, EDC_NAMESPACE).add(EDC_PREFIX, EDC_NAMESPACE).build()),
                    arguments(Json.createValue(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
            );
        }
    }

}

