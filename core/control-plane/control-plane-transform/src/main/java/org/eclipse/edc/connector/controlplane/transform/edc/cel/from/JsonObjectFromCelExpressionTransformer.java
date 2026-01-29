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

package org.eclipse.edc.connector.controlplane.transform.edc.cel.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_DESCRIPTION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SCOPES_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_TYPE_IRI;

public class JsonObjectFromCelExpressionTransformer extends AbstractJsonLdTransformer<CelExpression, JsonObject> {

    private final JsonBuilderFactory factory;

    public JsonObjectFromCelExpressionTransformer(JsonBuilderFactory factory) {
        super(CelExpression.class, JsonObject.class);
        this.factory = factory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CelExpression celExpression, @NotNull TransformerContext context) {
        return factory.createObjectBuilder()
                .add(ID, celExpression.getId())
                .add(TYPE, CEL_EXPRESSION_TYPE_IRI)
                .add(CEL_EXPRESSION_LEFT_OPERAND_IRI, celExpression.getLeftOperand())
                .add(CEL_EXPRESSION_EXPRESSION_IRI, celExpression.getExpression())
                .add(CEL_EXPRESSION_DESCRIPTION_IRI, celExpression.getDescription())
                .add(CEL_EXPRESSION_SCOPES_IRI, Json.createArrayBuilder(celExpression.getScopes()))
                .build();
    }
}
