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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_IDENTITY_IRI;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_PROPERTIES_IRI;

public class JsonObjectToParticipantContextTransformer extends AbstractJsonLdTransformer<JsonObject, ParticipantContext> {
    public JsonObjectToParticipantContextTransformer() {
        super(JsonObject.class, ParticipantContext.class);
    }

    @Override
    public @Nullable ParticipantContext transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var participantContext = ParticipantContext.Builder.newInstance();
        var nodeId = nodeId(jsonObject);
        var id = nodeId != null ? nodeId : UUID.randomUUID().toString();
        participantContext.participantContextId(id);
        participantContext.id(id);

        transformString(jsonObject.get(PARTICIPANT_CONTEXT_IDENTITY_IRI), participantContext::identity, context);

        var properties = jsonObject.get(PARTICIPANT_CONTEXT_PROPERTIES_IRI);
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> participantContext.property(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }

        return participantContext.build();
    }
}
