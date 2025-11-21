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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_PROPERTIES_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_STATE_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_TYPE_IRI;

public class JsonObjectFromParticipantContextTransformer extends AbstractJsonLdTransformer<ParticipantContext, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromParticipantContextTransformer(JsonBuilderFactory jsonFactory) {
        super(ParticipantContext.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ParticipantContext participantContext, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, PARTICIPANT_CONTEXT_TYPE_IRI)
                .add(ID, participantContext.getParticipantContextId())
                .add(PARTICIPANT_CONTEXT_PROPERTIES_IRI, createProperties(participantContext))
                .add(PARTICIPANT_CONTEXT_STATE_IRI, createId(jsonFactory, ParticipantContextState.from(participantContext.getState()).name()))
                .build();
    }

    private JsonObject createProperties(ParticipantContext participantContext) {
        return jsonFactory.createObjectBuilder()
                .add(VALUE, jsonFactory.createObjectBuilder(participantContext.getProperties()))
                .add(TYPE, JSON)
                .build();
    }
}
