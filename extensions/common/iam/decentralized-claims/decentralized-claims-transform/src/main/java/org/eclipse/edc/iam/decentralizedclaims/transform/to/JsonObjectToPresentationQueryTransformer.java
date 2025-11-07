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

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_SCOPE_TERM;

/**
 * Transforms a JsonObject into a PresentationQuery object.
 */
public class JsonObjectToPresentationQueryTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, PresentationQueryMessage> {

    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectToPresentationQueryTransformer(TypeManager typeManager, String typeContext) {
        this(typeManager, typeContext, DSPACE_DCP_NAMESPACE_V_0_8);
    }

    public JsonObjectToPresentationQueryTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(JsonObject.class, PresentationQueryMessage.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable PresentationQueryMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var bldr = PresentationQueryMessage.Builder.newinstance();

        var definition = jsonObject.get(forNamespace(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM));
        if (definition != null) {
            bldr.presentationDefinition(readPresentationDefinition(definition, context));
        }

        var scopes = jsonObject.get(forNamespace(PRESENTATION_QUERY_MESSAGE_SCOPE_TERM));
        if (scopes != null) {
            transformArrayOrObject(scopes, Object.class, o -> bldr.scopes(List.of(o.toString().split(" "))), context);
        }

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
            return typeManager.getMapper(typeContext).readValue(rawJson.toString(), PresentationDefinition.class);
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }
}
