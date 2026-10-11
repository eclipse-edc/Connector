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

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
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
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.LANGUAGE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
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
 *     <li>Json-Ld objects that contain an {@code @id} property and no {@code @value} are read as the {@code @id} string.</li>
 *     <li>All other Json-Ld forms are returned as-is.</li>
 * </ul>
 * In strict mode, used by the Management API, a right operand that is not a literal or an array of literals is
 * reported as a problem.
 */
public class JsonObjectToConstraintTransformer extends JsonLdToModelTransformer<JsonObject, Constraint> {

    private final Map<String, Supplier<MultiplicityConstraint.Builder<?, ?>>> operands = Map.of(
            ODRL_AND_CONSTRAINT_ATTRIBUTE, AndConstraint.Builder::newInstance,
            ODRL_OR_CONSTRAINT_ATTRIBUTE, OrConstraint.Builder::newInstance,
            ODRL_XONE_CONSTRAINT_ATTRIBUTE, XoneConstraint.Builder::newInstance
    );

    public static final String NON_LITERAL_RIGHT_OPERAND_MESSAGE = "rightOperand must be a literal (non-blank string, number, boolean or typed literal) " +
            "or an array of literals: use the plain string instead of {\"@id\": ...} or other objects";

    private static final Set<String> LITERAL_KEYS = Set.of(VALUE, TYPE, LANGUAGE);

    private final boolean strict;

    public JsonObjectToConstraintTransformer() {
        this(false);
    }

    public JsonObjectToConstraintTransformer(boolean strict) {
        super(JsonObject.class, Constraint.class);
        this.strict = strict;
    }

    /**
     * Returns true if the expanded right operand is a literal or a non-empty array of literals. A literal is a value
     * object whose {@code @value} is a string, a number or a boolean, optionally with an {@code @type} or {@code @language}.
     */
    public static boolean isLiteralRightOperand(JsonValue rightOperand) {
        if (rightOperand instanceof JsonArray array) {
            return !array.isEmpty() && array.stream().allMatch(JsonObjectToConstraintTransformer::isLiteral);
        }
        return isLiteral(rightOperand);
    }

    private static boolean isLiteral(JsonValue item) {
        if (!(item instanceof JsonObject object) || !LITERAL_KEYS.containsAll(object.keySet()) || JSON.equals(object.getString(TYPE, null))) {
            return false;
        }
        var value = object.get(VALUE);
        return value != null && switch (value.getValueType()) {
            case STRING -> !((JsonString) value).getString().isBlank();
            case NUMBER, TRUE, FALSE -> true;
            default -> false;
        };
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

        var jsonRightOperand = object.get(ODRL_RIGHT_OPERAND_ATTRIBUTE);
        if (strict && !isLiteralRightOperand(jsonRightOperand)) {
            context.reportProblem(NON_LITERAL_RIGHT_OPERAND_MESSAGE);
            return null;
        }

        var rightOperand = extractComplexValue(jsonRightOperand);

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
        if (root instanceof JsonArray array) {
            if (array.size() == 1) {
                return extractComplexValue(array.get(0));
            }
            return array.stream().map(this::extractComplexValue).toList();
        }

        if (root instanceof JsonObject object && !object.containsKey(VALUE) && object.get(ID) instanceof JsonString id) {
            return id.getString();
        }

        return extractValue(root);
    }

    /**
     * Extracts the value. If the value is a scalar, its Java representation is returned; otherwise the Json representation is returned.
     */
    private Object extractValue(JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT -> {
                var nestedValue = value.asJsonObject().get(VALUE);
                if (nestedValue != null) {
                    return extractValue(nestedValue);
                }
            }
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
        return value;
    }

}
