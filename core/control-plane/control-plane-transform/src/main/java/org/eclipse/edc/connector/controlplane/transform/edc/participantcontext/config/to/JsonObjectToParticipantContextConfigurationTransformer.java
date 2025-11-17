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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;

public class JsonObjectToParticipantContextConfigurationTransformer extends AbstractJsonLdTransformer<JsonObject, ParticipantContextConfiguration> {
    public JsonObjectToParticipantContextConfigurationTransformer() {
        super(JsonObject.class, ParticipantContextConfiguration.class);
    }

    @Override
    public @Nullable ParticipantContextConfiguration transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var participantContext = ParticipantContextConfiguration.Builder.newInstance();

        var properties = jsonObject.get(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI);
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> participantContext.entry(key, transformString(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }

        return participantContext.build();
    }
}
