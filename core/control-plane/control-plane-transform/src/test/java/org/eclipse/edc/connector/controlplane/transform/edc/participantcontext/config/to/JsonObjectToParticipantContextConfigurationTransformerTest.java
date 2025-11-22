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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.to;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonObjectToParticipantContextConfigurationTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToParticipantContextConfigurationTransformer transformer = new JsonObjectToParticipantContextConfigurationTransformer();

    @Test
    void transform_shouldConvertJsonObjectToParticipantContextConfig() {
        var jsonObject = createObjectBuilder()
                .add(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI, createObjectBuilder()
                        .add(VALUE, createObjectBuilder()
                                .add("key1", "value1")
                                .add("key2", "value2")))

                .add(PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI, createObjectBuilder()
                        .add(VALUE, createObjectBuilder()
                                .add("key1", "privateValue1")
                                .add("key2", "privateValue2")))
                .build();

        var result = transformer.transform(jsonObject, context);

        assertThat(result).isNotNull();
        assertThat(result.getEntries()).containsEntry("key1", "value1")
                .containsEntry("key2", "value2");

        assertThat(result).isNotNull();
        assertThat(result.getPrivateEntries()).containsEntry("key1", "privateValue1")
                .containsEntry("key2", "privateValue2");

    }

    @Test
    void transform_withEmptyProperties_shouldReturnParticipantContextConfigWithEmptyEntries() {
        var jsonObject = createObjectBuilder()
                .add(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI, createObjectBuilder().add(VALUE, createObjectBuilder()))
                .build();

        var result = transformer.transform(jsonObject, context);
        assertThat(result).isNotNull();
        assertThat(result.getEntries()).isEmpty();

    }

    @Test
    void transform_withoutEntries_shouldReturnParticipantContextConfig() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-3")
                .build();

        var result = transformer.transform(jsonObject, context);
        assertThat(result).isNotNull();
        assertThat(result.getEntries()).isEmpty();

    }

    @Test
    void transform_withInvalidEntries_shouldReportProblemAndReturnNull() {
        var jsonObject = createObjectBuilder()
                .add(ID, "participant-4")
                .add(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI, createObjectBuilder().add(VALUE, "invalid-string"))
                .build();

        var result = transformer.transform(jsonObject, context);
        assertThat(result).isNull();

        verify(context).reportProblem(contains("Expected properties to be a JsonObject"));
    }
}
