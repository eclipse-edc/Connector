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
import org.eclipse.edc.policy.cel.model.CelExpressionTestRequest;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_EXPRESSION_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_LEFT_OPERAND_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_OPERATOR_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_PARAMS_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_RIGHT_OPERAND_IRI;

public class JsonObjectToCelExpressionTestRequestTransformer extends AbstractJsonLdTransformer<JsonObject, CelExpressionTestRequest> {

    public JsonObjectToCelExpressionTestRequestTransformer() {
        super(JsonObject.class, CelExpressionTestRequest.class);
    }

    @Override
    public @Nullable CelExpressionTestRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {


        var expression = transformString(object.get(CEL_EXPRESSION_TEST_REQUEST_EXPRESSION_IRI), context);
        var operandLeft = transformString(object.get(CEL_EXPRESSION_TEST_REQUEST_LEFT_OPERAND_IRI), context);
        var operandRight = transformGenericProperty(object.get(CEL_EXPRESSION_TEST_REQUEST_RIGHT_OPERAND_IRI), context);
        var operator = transformString(object.get(CEL_EXPRESSION_TEST_REQUEST_OPERATOR_IRI), context);

        var builder = CelExpressionTestRequest.Builder.newInstance()
                .leftOperand(operandLeft)
                .rightOperand(operandRight)
                .operator(operator)
                .expression(expression);

        var params = object.get(CEL_EXPRESSION_TEST_REQUEST_PARAMS_IRI);
        if (params != null) {
            var jsonValue = nodeJsonValue(params);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> builder.param(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;
            }
        }
        return builder.build();
    }
}
