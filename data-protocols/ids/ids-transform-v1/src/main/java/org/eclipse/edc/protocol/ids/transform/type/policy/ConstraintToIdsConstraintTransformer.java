/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *       Fraunhofer Insitute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintBuilder;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConstraintToIdsConstraintTransformer implements IdsTypeTransformer<Constraint, de.fraunhofer.iais.eis.Constraint> {

    @Override
    public Class<Constraint> getInputType() {
        return Constraint.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Constraint> getOutputType() {
        return de.fraunhofer.iais.eis.Constraint.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Constraint transform(@NotNull Constraint constraint, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (constraint == null) {
            return null;
        }

        if (constraint instanceof AtomicConstraint) {
            return transformAtomicConstraint((AtomicConstraint) constraint, context);
        } else {
            context.reportProblem(String.format("An IDS constraint requires an AtomicConstraint as input: %s", constraint.getClass().getName()));
            return null;
        }

    }

    private de.fraunhofer.iais.eis.Constraint transformAtomicConstraint(AtomicConstraint constraint, @NotNull TransformerContext context) {
        var leftOperand = context.transform(constraint.getLeftExpression(), String.class);
        var rightOperand = context.transform(constraint.getRightExpression(), RdfResource.class);
        var operator = context.transform(constraint.getOperator(), BinaryOperator.class);

        var id = IdsId.Builder.newInstance().value(constraint.hashCode()).type(IdsType.CONSTRAINT).build().toUri();
        var builder = new IdsConstraintBuilder(id);

        builder.leftOperand(leftOperand);
        builder.rightOperand(rightOperand);
        builder.operator(operator);

        return builder.build();
    }


}
