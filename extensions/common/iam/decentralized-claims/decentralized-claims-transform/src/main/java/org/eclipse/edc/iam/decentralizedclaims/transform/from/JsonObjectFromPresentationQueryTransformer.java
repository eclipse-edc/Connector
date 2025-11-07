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

package org.eclipse.edc.iam.decentralizedclaims.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_SCOPE_TERM;
import static org.eclipse.edc.iam.decentralizedclaims.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromPresentationQueryTransformer extends AbstractNamespaceAwareJsonLdTransformer<PresentationQueryMessage, JsonObject> {
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromPresentationQueryTransformer(TypeManager typeManager, String typeContext) {
        this(typeManager, typeContext, DSPACE_DCP_NAMESPACE_V_0_8);
    }

    public JsonObjectFromPresentationQueryTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(PresentationQueryMessage.class, JsonObject.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull PresentationQueryMessage presentationQueryMessage, @NotNull TransformerContext context) {
        var builder = Json.createObjectBuilder()
                .add(TYPE, forNamespace(PRESENTATION_QUERY_MESSAGE_TERM));

        if (presentationQueryMessage.getPresentationDefinition() != null) {
            var presentationDefinition = Json.createObjectBuilder();
            presentationDefinition.add(JsonLdKeywords.VALUE, typeManager.getMapper(typeContext).convertValue(presentationQueryMessage.getPresentationDefinition(), JsonObject.class));
            presentationDefinition.add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON);
            builder.add(forNamespace(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM), presentationDefinition);
        } else {
            builder.add(forNamespace(PRESENTATION_QUERY_MESSAGE_SCOPE_TERM), Json.createArrayBuilder(presentationQueryMessage.getScopes()));
        }
        return builder.build();

    }
}
