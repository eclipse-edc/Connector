/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.to;

import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToDataplaneMetadataTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_PROPERTIES_IRI;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToParticipantContextTransformerTest {

    private final TypeManager typeManager = mock();
    private TypeTransformerRegistry typeTransformerRegistry;

    @BeforeEach
    void setUp() {
        var transformer = new JsonObjectToParticipantContextTransformer();
        typeTransformerRegistry = new TypeTransformerRegistryImpl();
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        typeTransformerRegistry.register(transformer);
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataplaneMetadataTransformer());
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform_shouldConvertJsonObjectToParticipantContext() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-1")
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createObjectBuilder()
                        .add(VALUE, createObjectBuilder()
                                .add("key1", "value1")
                                .add("key2", "value2")))
                .build();

        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isSucceeded()
                .satisfies(participantContext -> {
                    assertThat(participantContext.getParticipantContextId()).isEqualTo("participant-1");
                    assertThat(participantContext.getProperties()).containsEntry("key1", "value1")
                            .containsEntry("key2", "value2");
                });
    }

    @Test
    void transform_withoutId_shouldGenerateRandomId() {
        var jsonObject = createObjectBuilder()
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createObjectBuilder().add(VALUE, createObjectBuilder()
                        .add("key1", "value1")))
                .build();

        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isSucceeded().satisfies(participantContext -> {
            assertThat(participantContext.getParticipantContextId()).isNotNull().isNotEmpty();
            assertThat(participantContext.getProperties()).containsEntry("key1", "value1");
        });
    }

    @Test
    void transform_withEmptyProperties_shouldReturnParticipantContextWithEmptyProperties() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-2")
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createObjectBuilder().add(VALUE, createObjectBuilder()))
                .build();

        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isSucceeded().satisfies(participantContext -> {
            assertThat(participantContext.getParticipantContextId()).isNotNull().isNotEmpty();
            assertThat(participantContext.getProperties()).isEmpty();
        });
    }

    @Test
    void transform_withoutProperties_shouldReturnParticipantContext() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-3")
                .build();

        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isSucceeded().satisfies(participantContext -> {
            assertThat(participantContext.getParticipantContextId()).isEqualTo("participant-3");
            assertThat(participantContext.getProperties()).isEmpty();
        });

    }

    @Test
    void transform_withInvalidProperties_shouldReportProblemAndReturnNull() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-4")
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createObjectBuilder().add(VALUE, "invalid-string"))
                .build();

        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isFailed().detail().contains("Expected properties to be a JsonObject");
    }

    @Test
    void transform_withNestedProperties_shouldHandleComplexValues() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-5")
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createObjectBuilder()
                        .add(VALUE, createObjectBuilder()
                                .add("simpleKey", "simpleValue")
                                .add("booleanKey", true)))
                .build();


        var result = typeTransformerRegistry.transform(jsonObject, ParticipantContext.class);

        assertThat(result).isSucceeded().satisfies(participantContext -> {
            assertThat(participantContext.getParticipantContextId()).isEqualTo("participant-5");
            assertThat(participantContext.getProperties()).containsEntry("simpleKey", "simpleValue")
                    .containsEntry("booleanKey", true);
        });
    }
}
