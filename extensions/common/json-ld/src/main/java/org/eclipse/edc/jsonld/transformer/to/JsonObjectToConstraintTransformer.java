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

import jakarta.json.JsonObject;
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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;

/**
 * Converts from an ODRL constraint as a {@link JsonObject} in JSON-LD expanded form to a {@link Constraint}.
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
                    .type("Constraint")
                    .property(ODRL_LEFT_OPERAND_ATTRIBUTE)
                    .report();
            return null;
        }

        if (!transformMandatoryString(object.get(ODRL_OPERATOR_ATTRIBUTE), s -> builder.operator(Operator.valueOf(s)), context)) {
            context.problem()
                    .missingProperty()
                    .type(ODRL_CONSTRAINT_TYPE)
                    .property(ODRL_LEFT_OPERAND_ATTRIBUTE)
                    .report();
            return null;
        }

        var rightOperand = transformString(object.get(ODRL_RIGHT_OPERAND_ATTRIBUTE), context);
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
}
