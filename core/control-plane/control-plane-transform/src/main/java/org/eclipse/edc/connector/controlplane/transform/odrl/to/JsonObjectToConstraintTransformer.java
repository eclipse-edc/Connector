/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;

/**
 * Converts from an ODRL constraint as a {@link JsonObject} in JSON-LD expanded form to a {@link Constraint}.
 * <p>
 * Right operands of atomic constraints are transformed as follows:
 * <ul>
 *     <li>Json-Ld expanded scalar types are extracted from their enclosing array and passed directly to the constraint evaluator.</li>
 *     <li>Json-Ld arrays that contain a single element (Json-Ld scalar or complex type) will return the extracted element. </li>
 *     <li>Json-Ld arrays that contain multiple elements will be returned as-is.</li>
 *     <li>Json-Ld objects that contain an @code{value} property will have that property extracted and returned.</li>
 *     <li>All other Json-Ld forms are returned as-is.</li>
 * </ul>
 * -
 */
public class JsonObjectToConstraintTransformer extends AbstractJsonLdTransformer<JsonObject, Constraint> {

    private final Map<String, Supplier<MultiplicityConstraint.Builder<?, ?>>> operands = Map.of(
            ODRL_AND_CONSTRAINT_ATTRIBUTE, AndConstraint.Builder::newInstance,
            ODRL_OR_CONSTRAINT_ATTRIBUTE, OrConstraint.Builder::newInstance,
            ODRL_XONE_CONSTRAINT_ATTRIBUTE, XoneConstraint.Builder::newInstance
    );

    public JsonObjectToConstraintTransformer() {
        super(JsonObject.class, Constraint.class);
    }

    @Override
    public @Nullable Constraint transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var logicalConstraint = transformLogicalConstraint(object, context);

        if (logicalConstraint != null) {
            return logicalConstraint;
        }

        return transformAtomicConstraint(object, context);
    }

    private AtomicConstraint transformAtomicConstraint(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = AtomicConstraint.Builder.newInstance();

        if (!transformMandatoryString(object.get(ODRL_LEFT_OPERAND_ATTRIBUTE), s -> builder.leftExpression(new LiteralExpression(s)), context)) {
            context.problem()
                    .missingProperty()
                    .type(ODRL_CONSTRAINT_TYPE)
                    .property(ODRL_LEFT_OPERAND_ATTRIBUTE)
                    .report();
            return null;
        }

        var jsonOperator = object.get(ODRL_OPERATOR_ATTRIBUTE);
        if (jsonOperator == null) {
            context.problem()
                    .missingProperty()
                    .type(ODRL_CONSTRAINT_TYPE)
                    .property(ODRL_OPERATOR_ATTRIBUTE)
                    .report();
            return null;
        }

        builder.operator(transformObject(jsonOperator, Operator.class, context));

        var rightOperand = extractComplexValue(object.get(ODRL_RIGHT_OPERAND_ATTRIBUTE));

        builder.rightExpression(new LiteralExpression(rightOperand));

        return builderResult(builder::build, context);
    }

    @Nullable
    private MultiplicityConstraint transformLogicalConstraint(@NotNull JsonObject object, @NotNull TransformerContext context) {
        return operands.entrySet().stream()
                .filter(entry -> object.containsKey(entry.getKey()))
                .findFirst()
                .map(entry -> {
                    var builder = entry.getValue().get()
                            .constraints(transformArray(object.get(entry.getKey()), Constraint.class, context));
                    return builderResult(builder::build, context);
                })
                .orElse(null);
    }

    /**
     * Extracts a value from a root type according to the rules specified by this class.
     *
     * @param root the root object to extract the value from.
     * @return the extracted value
     */
    private Object extractComplexValue(JsonValue root) {
        switch (root.getValueType()) {
            case ARRAY -> {
                var array = root.asJsonArray();
                if (array.size() != 1) {
                    // not a single element array, return as-is
                    return array;
                }
                // single element array, extract and return it
                return extractComplexValue(array.get(0));
            }
            case OBJECT -> {
                var valueProp = root.asJsonObject().get(VALUE);
                if (valueProp != null) {
                    // object has a value type, extract and return it
                    return extractValue(valueProp);
                }
            }
            default -> {
                // extract the value directly
                return extractValue(root);
            }
        }

        return extractValue(root);
    }

    /**
     * Extracts the value. If the value is a scalar, its Java representation is returned; otherwise the Json representation is returned.
     */
    private Object extractValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING -> {
                return ((JsonString) value).getString();
            }
            case NUMBER -> {
                return ((JsonNumber) value).numberValue();
            }
            case TRUE -> {
                return true;
            }
            case FALSE -> {
                return false;
            }
            default -> {
                return value;
            }
        }
    }

}
