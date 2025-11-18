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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromParticipantContextConfigurationTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private JsonObjectFromParticipantContextConfigurationTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromParticipantContextConfigurationTransformer(jsonFactory);
        context = mock(TransformerContext.class);
    }

    @Test
    void transform_shouldConvertParticipantContextConfigurationToJsonObject() {
        var config = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant-1")
                .entries(Map.of("key1", "value1", "key2", "value2"))
                .build();

        var result = transformer.transform(config, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI);

        var properties = result.getJsonObject(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI).getJsonObject(VALUE);
        assertThat(properties).isNotNull();
        assertThat(properties.getString("key1")).isEqualTo("value1");
        assertThat(properties.getString("key2")).isEqualTo("value2");
    }

    @Test
    void transform_withEmptyEntries_shouldReturnJsonObjectWithEmptyEntries() {
        var config = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participant-2")
                .entries(Map.of())
                .build();

        var result = transformer.transform(config, context);

        assertThat(result).isNotNull();
        var properties = result.getJsonObject(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI).getJsonObject(VALUE);
        assertThat(properties).isNotNull().isEmpty();
    }

}
