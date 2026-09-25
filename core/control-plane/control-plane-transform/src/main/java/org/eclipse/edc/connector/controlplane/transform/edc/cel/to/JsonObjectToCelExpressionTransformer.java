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
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_ACTIONS_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_DESCRIPTION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SCOPES_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_SUPPORTED_OPERATORS_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_TYPE_TERM;

public class JsonObjectToCelExpressionTransformer extends JsonLdToModelTransformer<JsonObject, CelExpression> {

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
        var actions = new HashSet<String>();
        var supportedOperators = new HashSet<Operator>();

        var operandLeft = transformString(object.get(CEL_EXPRESSION_LEFT_OPERAND_IRI), context);
        var expression = transformString(object.get(CEL_EXPRESSION_EXPRESSION_IRI), context);
        var description = transformString(object.get(CEL_EXPRESSION_DESCRIPTION_IRI), context);

        ofNullable(object.getJsonArray(CEL_EXPRESSION_SCOPES_IRI))
                .ifPresent(ja -> scopes.addAll(ja.stream().map(this::nodeValue).toList()));

        ofNullable(object.getJsonArray(CEL_EXPRESSION_ACTIONS_IRI))
                .ifPresent(ja -> actions.addAll(ja.stream().map(this::nodeValue).toList()));

        var operators = ofNullable(object.getJsonArray(CEL_EXPRESSION_SUPPORTED_OPERATORS_IRI))
                .map(ja -> ja.stream().map(this::nodeValue).toList())
                .orElse(List.of());
        for (var operator : operators) {
            try {
                supportedOperators.add(Operator.fromString(operator));
            } catch (IllegalArgumentException e) {
                context.problem().invalidProperty()
                        .type(CEL_EXPRESSION_TYPE_TERM)
                        .property(CEL_EXPRESSION_SUPPORTED_OPERATORS_IRI)
                        .value(operator)
                        .error("unknown operator")
                        .report();
                return null;
            }
        }

        return CelExpression.Builder.newInstance()
                .id(id)
                .scopes(scopes)
                .leftOperand(operandLeft)
                .expression(expression)
                .description(description)
                .actions(actions)
                .supportedOperators(supportedOperators)
                .build();
    }

}
