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
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;

/**
 * Converts from an ODRL constraint as a {@link JsonObject} in JSON-LD expanded form to a {@link Constraint}.
 */
public class JsonObjectToConstraintTransformer extends AbstractJsonLdTransformer<JsonObject, Constraint> {

    public JsonObjectToConstraintTransformer() {
        super(JsonObject.class, Constraint.class);
    }
    
    @Override
    public @Nullable Constraint transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        if (object.containsKey(ODRL_OPERAND_ATTRIBUTE)) {
            // logical/multiplicity constraint
            return context.transform(object, MultiplicityConstraint.class);
        } else if (object.containsKey(ODRL_OPERATOR_ATTRIBUTE)) {
            // atomic constraint
            return context.transform(object, AtomicConstraint.class);
        } else {
            context.reportProblem(format("Failed to identify constraint type. Neither of these properties found: %s, %s",
                    ODRL_OPERAND_ATTRIBUTE, ODRL_OPERATOR_ATTRIBUTE));
            return null;
        }
    }
}
