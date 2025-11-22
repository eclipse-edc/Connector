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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_IDENTITY_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_PROPERTIES_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_STATE_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_TYPE_IRI;
import static org.mockito.Mockito.mock;

class JsonObjectFromParticipantContextTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private JsonObjectFromParticipantContextTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromParticipantContextTransformer(jsonFactory);
        context = mock(TransformerContext.class);
    }

    @Test
    void transform_shouldConvertParticipantContextToJsonObject() {
        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participant-1")
                .state(ParticipantContextState.ACTIVATED)
                .identity("did:example:123")
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();

        var result = transformer.transform(participantContext, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("participant-1");
        assertThat(result.getString(PARTICIPANT_CONTEXT_IDENTITY_IRI)).isEqualTo("did:example:123");
        assertThat(result.getString(TYPE)).isEqualTo(PARTICIPANT_CONTEXT_TYPE_IRI);
        assertThat(result.getJsonObject(PARTICIPANT_CONTEXT_STATE_IRI).getString(ID)).isEqualTo("ACTIVATED");

        var properties = result.getJsonObject(PARTICIPANT_CONTEXT_PROPERTIES_IRI).getJsonObject(VALUE);
        assertThat(properties).isNotNull();
        assertThat(properties.getString("key1")).isEqualTo("value1");
        assertThat(properties.getString("key2")).isEqualTo("value2");
    }

    @Test
    void transform_withEmptyProperties_shouldReturnJsonObjectWithEmptyProperties() {
        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participant-2")
                .state(ParticipantContextState.ACTIVATED)
                .identity("did:example:123")
                .properties(Map.of())
                .build();

        var result = transformer.transform(participantContext, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("participant-2");
        var properties = result.getJsonObject(PARTICIPANT_CONTEXT_PROPERTIES_IRI).getJsonObject(VALUE);
        assertThat(properties).isNotNull().isEmpty();
    }

    @Test
    void transform_shouldIncludeCorrectState() {
        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participant-3")
                .state(ParticipantContextState.DEACTIVATED)
                .identity("did:example:123")
                .build();

        var result = transformer.transform(participantContext, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonObject(PARTICIPANT_CONTEXT_STATE_IRI).getString(ID)).isEqualTo("DEACTIVATED");
    }
}
