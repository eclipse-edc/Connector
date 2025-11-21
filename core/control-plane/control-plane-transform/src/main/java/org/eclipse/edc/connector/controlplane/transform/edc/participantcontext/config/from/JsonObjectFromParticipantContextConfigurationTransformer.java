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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI;

public class JsonObjectFromParticipantContextConfigurationTransformer extends AbstractJsonLdTransformer<ParticipantContextConfiguration, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromParticipantContextConfigurationTransformer(JsonBuilderFactory jsonFactory) {
        super(ParticipantContextConfiguration.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ParticipantContextConfiguration config, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI)
                .add(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI, createProperties(config.getEntries()))
                .add(PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI, createProperties(config.getPrivateEntries()))
                .build();
    }

    private JsonObject createProperties(Map<String, String> config) {
        var entries = jsonFactory.createObjectBuilder();
        config.forEach(entries::add);

        return jsonFactory.createObjectBuilder()
                .add(VALUE, entries)
                .add(TYPE, JSON)
                .build();
    }
}
