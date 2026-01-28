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

package org.eclipse.edc.connector.controlplane.transform.edc.cel.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_DESCRIPTION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SCOPES_IRI;

public class JsonObjectToCelExpressionTransformer extends AbstractJsonLdTransformer<JsonObject, CelExpression> {

    public JsonObjectToCelExpressionTransformer() {
        super(JsonObject.class, CelExpression.class);
    }

    @Override
    public @Nullable CelExpression transform(@NotNull JsonObject object, @NotNull TransformerContext context) {

        var id = nodeId(object);

        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        var scopes = new HashSet<String>();

        var operandLeft = transformString(object.get(CEL_EXPRESSION_LEFT_OPERAND_IRI), context);
        var expression = transformString(object.get(CEL_EXPRESSION_EXPRESSION_IRI), context);
        var description = transformString(object.get(CEL_EXPRESSION_DESCRIPTION_IRI), context);

        ofNullable(object.getJsonArray(CEL_EXPRESSION_SCOPES_IRI))
                .ifPresent(ja -> scopes.addAll(ja.stream().map(this::nodeValue).toList()));

        return CelExpression.Builder.newInstance()
                .id(id)
                .scopes(scopes)
                .leftOperand(operandLeft)
                .expression(expression)
                .description(description)
                .build();
    }
}
