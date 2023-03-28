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
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;

public class JsonObjectToConstraintTransformer extends AbstractJsonLdTransformer<JsonObject, Constraint> {

    private static final String ODRL_LEFT_OPERAND_PROPERTY = ODRL_SCHEMA + "leftOperand";
    private static final String ODRL_OPERATOR_PROPERTY = ODRL_SCHEMA + "operator";
    private static final String ODRL_RIGHT_OPERAND_PROPERTY = ODRL_SCHEMA + "rightOperand";

    public JsonObjectToConstraintTransformer() {
        super(JsonObject.class, Constraint.class);
    }

    @Override
    public @Nullable Constraint transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        //TODO check for type of constraint (atomic, and, or, ...)
        var builder = AtomicConstraint.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }

    private void transformProperties(String key, JsonValue value, AtomicConstraint.Builder builder, TransformerContext context) {
        if (ODRL_LEFT_OPERAND_PROPERTY.equals(key)) {
            transformOperand(value, builder::leftExpression, context);
        } else if (ODRL_OPERATOR_PROPERTY.equals(key)) {
            transformOperator(value, builder, context);
        } else if (ODRL_RIGHT_OPERAND_PROPERTY.equals(key)) {
            transformOperand(value, builder::rightExpression, context);
        }
    }

    private void transformOperand(JsonValue value, Consumer<LiteralExpression> builderFunction, TransformerContext context) {
        if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            var operand = new LiteralExpression(object.getString(VALUE));
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
            var string = (JsonString) value;
            var operator = Operator.valueOf(string.getString());
            builder.operator(operator);
        } else if (value instanceof JsonObject) {
            var object = (JsonObject) value;
            transformOperator(object.getJsonString(VALUE), builder, context);
        } else if (value instanceof JsonArray) {
            var array = (JsonArray) value;
            transformOperator(array.getJsonObject(0), builder, context);
        } else {
            context.reportProblem("Invalid operator property");
        }
    }
}
