/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQuery;
import org.eclipse.edc.identitytrust.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transforms a JsonObject into a PresentationQuery object.
 */
public class JsonObjectToPresentationQueryTransformer extends AbstractJsonLdTransformer<JsonObject, PresentationQuery> {

    private final ObjectMapper mapper;

    public JsonObjectToPresentationQueryTransformer(ObjectMapper mapper) {
        super(JsonObject.class, PresentationQuery.class);
        this.mapper = mapper;
    }

    @Override
    public @Nullable PresentationQuery transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var bldr = PresentationQuery.Builder.newinstance();
        visitProperties(jsonObject, (k, v) -> {
            switch (k) {
                case PresentationQuery.PRESENTATION_QUERY_DEFINITION_PROPERTY ->
                        bldr.presentationDefinition(readPresentationDefinition(v, context));
                case PresentationQuery.PRESENTATION_QUERY_SCOPE_PROPERTY ->
                        transformArrayOrObject(v, Object.class, o -> bldr.scope(o.toString()), context);
                default -> context.reportProblem("Unknown property '%s'".formatted(k));
            }
        });

        return bldr.build();
    }

    private PresentationDefinition readPresentationDefinition(JsonValue v, TransformerContext context) {
        JsonObject jo;
        if (v.getValueType() == JsonValue.ValueType.ARRAY && !((JsonArray) v).isEmpty()) {
            jo = v.asJsonArray().getJsonObject(0);
        } else {
            jo = v.asJsonObject();
        }
        var rawJson = jo.get(JsonLdKeywords.VALUE);
        try {
            return mapper.readValue(rawJson.toString(), PresentationDefinition.class);
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }
}
