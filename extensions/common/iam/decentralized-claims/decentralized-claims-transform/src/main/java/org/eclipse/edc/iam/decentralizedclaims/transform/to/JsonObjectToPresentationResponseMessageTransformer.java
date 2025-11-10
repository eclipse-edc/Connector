/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationResponseMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationResponseMessage.Builder;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_SUBMISSION_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationResponseMessage.PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM;

/**
 * Transforms a {@link JsonObject} into a {@link PresentationResponseMessage} object.
 */
public class JsonObjectToPresentationResponseMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, PresentationResponseMessage> {

    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectToPresentationResponseMessageTransformer(TypeManager typeManager, String typeContext) {
        this(typeManager, typeContext, DSPACE_DCP_NAMESPACE_V_0_8);
    }

    public JsonObjectToPresentationResponseMessageTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(JsonObject.class, PresentationResponseMessage.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable PresentationResponseMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Builder.newinstance();
        var submission = jsonObject.get(forNamespace(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_SUBMISSION_TERM));
        if (submission != null) {
            builder.presentationSubmission(readPresentationSubmission(submission, context));
        }

        var presentation = jsonObject.get(forNamespace(PRESENTATION_RESPONSE_MESSAGE_PRESENTATION_TERM));
        if (presentation != null) {
            builder.presentation(readPresentation(presentation, context));
        }

        return builder.build();
    }

    private PresentationSubmission readPresentationSubmission(JsonValue v, TransformerContext context) {
        var rawJson = getRawJsonValue(v);
        try {
            return typeManager.getMapper(typeContext).readValue(rawJson.toString(), PresentationSubmission.class);
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }


    private List<Object> readPresentation(JsonValue v, TransformerContext context) {
        var rawJson = getRawJsonValue(v);
        try {
            return typeManager.getMapper(typeContext).readValue(rawJson.toString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }

    private JsonValue getRawJsonValue(JsonValue v) {
        JsonObject jo;
        if (v.getValueType() == JsonValue.ValueType.ARRAY && !((JsonArray) v).isEmpty()) {
            jo = v.asJsonArray().getJsonObject(0);
        } else {
            jo = v.asJsonObject();
        }
        return jo.get(JsonLdKeywords.VALUE);
    }

}
