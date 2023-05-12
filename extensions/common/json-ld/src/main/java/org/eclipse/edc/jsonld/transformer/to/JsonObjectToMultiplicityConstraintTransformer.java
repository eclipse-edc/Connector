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
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;

/**
 * Converts from an ODRL logical constraint as a {@link JsonObject} in JSON-LD expanded form to a {@link MultiplicityConstraint}.
 */
public class JsonObjectToMultiplicityConstraintTransformer extends AbstractJsonLdTransformer<JsonObject, MultiplicityConstraint> {

    public JsonObjectToMultiplicityConstraintTransformer() {
        super(JsonObject.class, MultiplicityConstraint.class);
    }

    @Override
    public @Nullable MultiplicityConstraint transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var operand = returnMandatoryJsonObject(object.get(ODRL_OPERAND_ATTRIBUTE), context, ODRL_OPERAND_ATTRIBUTE);
        if (operand == null) {
            return null;
        }
        if (operand.containsKey(ODRL_AND_CONSTRAINT_ATTRIBUTE)) {
            var builder = transformConstraints(operand.getJsonArray(ODRL_AND_CONSTRAINT_ATTRIBUTE), AndConstraint.Builder.newInstance(), context);
            return builderResult(builder::build, context);
        } else if (operand.containsKey(ODRL_OR_CONSTRAINT_ATTRIBUTE)) {
            var builder = transformConstraints(operand.getJsonArray(ODRL_OR_CONSTRAINT_ATTRIBUTE), OrConstraint.Builder.newInstance(), context);
            return builderResult(builder::build, context);
        } else if (operand.containsKey(ODRL_XONE_CONSTRAINT_ATTRIBUTE)) {
            var builder = transformConstraints(operand.getJsonArray(ODRL_XONE_CONSTRAINT_ATTRIBUTE), XoneConstraint.Builder.newInstance(), context);
            return builderResult(builder::build, context);
        } else {
            context.reportProblem(format("Unsupported logical constraint encountered: expected one of [%s, %s, %s] but got [%s]",
                    ODRL_AND_CONSTRAINT_ATTRIBUTE, ODRL_OR_CONSTRAINT_ATTRIBUTE, ODRL_XONE_CONSTRAINT_ATTRIBUTE,
                    join(", ", operand.keySet())));
            return null;
        }
    }

    private MultiplicityConstraint.Builder<?, ?> transformConstraints(JsonArray constraints, MultiplicityConstraint.Builder<?, ?> builder, TransformerContext context) {
        if (constraints != null) {
            constraints.forEach(c -> builder.constraint(context.transform(c, Constraint.class)));
        }

        return builder;
    }

}
