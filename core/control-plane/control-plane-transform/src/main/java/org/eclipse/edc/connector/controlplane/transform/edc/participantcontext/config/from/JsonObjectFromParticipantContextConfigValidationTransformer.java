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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdFromModelTransformer;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_KEY_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfigValidation.PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_MESSAGE_IRI;

public class JsonObjectFromParticipantContextConfigValidationTransformer extends JsonLdFromModelTransformer<ParticipantContextConfigValidation, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromParticipantContextConfigValidationTransformer(JsonBuilderFactory jsonFactory) {
        super(ParticipantContextConfigValidation.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ParticipantContextConfigValidation validation, @NotNull TransformerContext context) {
        var violations = jsonFactory.createArrayBuilder();
        validation.violations().forEach(violation -> violations.add(jsonFactory.createObjectBuilder()
                .add(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_KEY_IRI, violation.path())
                .add(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATION_MESSAGE_IRI, violation.message())));

        return jsonFactory.createObjectBuilder()
                .add(ID, validation.participantContextId())
                .add(TYPE, PARTICIPANT_CONTEXT_CONFIG_VALIDATION_TYPE_IRI)
                .add(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VALID_IRI, validation.isValid())
                .add(PARTICIPANT_CONTEXT_CONFIG_VALIDATION_VIOLATIONS_IRI, violations)
                .build();
    }
}
