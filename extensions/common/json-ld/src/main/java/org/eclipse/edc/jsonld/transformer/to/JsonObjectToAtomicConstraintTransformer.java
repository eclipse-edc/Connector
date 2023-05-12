/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;

/**
 * Converts from an ODRL constraint as a {@link JsonObject} in JSON-LD expanded form to an {@link AtomicConstraint}.
 */
public class JsonObjectToAtomicConstraintTransformer extends AbstractJsonLdTransformer<JsonObject, AtomicConstraint> {

    public JsonObjectToAtomicConstraintTransformer() {
        super(JsonObject.class, AtomicConstraint.class);
    }

    @Override
    public @Nullable AtomicConstraint transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = AtomicConstraint.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        if (context.hasProblems()) {
            return null;
        }
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue value, AtomicConstraint.Builder builder, TransformerContext context) {
        if (ODRL_LEFT_OPERAND_ATTRIBUTE.equals(key)) {
            transformOperand(value, builder::leftExpression, context);
        } else if (ODRL_OPERATOR_ATTRIBUTE.equals(key)) {
            transformOperator(value, builder, context);
        } else if (ODRL_RIGHT_OPERAND_ATTRIBUTE.equals(key)) {
            transformOperand(value, builder::rightExpression, context);
        }
    }

    private void transformOperand(JsonValue value, Consumer<LiteralExpression> builderFunction, TransformerContext context) {
        if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            var result = transformString(object, context);
            if (result == null) {
                return;
            }
            var operand = new LiteralExpression(result);
            builderFunction.accept(operand);
        } else if (value instanceof JsonArray) {
            var array = (JsonArray) value;
            transformOperand(array.getJsonObject(0), builderFunction, context);
        } else {
            context.reportProblem("Invalid operand property");
        }
    }

    private void transformOperator(JsonValue value, AtomicConstraint.Builder builder, TransformerContext context) {
        if (value instanceof JsonString) {
            transformOperator(((JsonString) value).getString(), builder, context);
        } else if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            transformOperator(object.get(VALUE), builder, context);
        } else if (value instanceof JsonArray) {
            var array = (JsonArray) value;
            transformOperator(array.getJsonObject(0), builder, context);
        } else {
            context.reportProblem("Invalid operator property: " + value);
        }
    }

    private void transformOperator(String value, AtomicConstraint.Builder builder, TransformerContext context) {
        try {
            builder.operator(Operator.valueOf(value));
        } catch (IllegalArgumentException e) {
            context.reportProblem("Invalid operator type: " + value);
        }
    }
}
