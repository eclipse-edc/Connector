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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.from;

import jakarta.json.Json;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_KEY_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_MESSAGE_IRI;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.Mockito.mock;

class JsonObjectFromParticipantContextConfigValidationTransformerTest {

    private final JsonObjectFromParticipantContextConfigValidationTransformer transformer =
            new JsonObjectFromParticipantContextConfigValidationTransformer(Json.createBuilderFactory(Map.of()));
    private final TransformerContext context = mock();

    @Test
    void transform_withViolations() {
        var validation = new ParticipantContextConfigValidation("participant-1", List.of(violation("'key' is required", "key")));

        var result = transformer.transform(validation, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("participant-1");
        assertThat(result.getString(TYPE)).isEqualTo(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_IRI);
        assertThat(result.getBoolean(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI)).isFalse();
        var violations = result.getJsonArray(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI);
        assertThat(violations).hasSize(1);
        assertThat(violations.getJsonObject(0).getString(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_KEY_IRI)).isEqualTo("key");
        assertThat(violations.getJsonObject(0).getString(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_MESSAGE_IRI)).isEqualTo("'key' is required");
    }

    @Test
    void transform_whenValid() {
        var result = transformer.transform(new ParticipantContextConfigValidation("participant-1", List.of()), context);

        assertThat(result).isNotNull();
        assertThat(result.getBoolean(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI)).isTrue();
        assertThat(result.getJsonArray(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI)).isEmpty();
    }
}
