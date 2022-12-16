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

import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintImpl;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConstraintFromIdsConstraintTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.Constraint, Constraint> {

    @Override
    public Class<de.fraunhofer.iais.eis.Constraint> getInputType() {
        return de.fraunhofer.iais.eis.Constraint.class;
    }

    @Override
    public Class<Constraint> getOutputType() {
        return Constraint.class;
    }

    @Override
    public @Nullable Constraint transform(de.fraunhofer.iais.eis.@NotNull Constraint constraint, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (constraint == null) {
            return null;
        }

        Operator operator = null;
        var binaryOperator = constraint.getOperator();
        if (binaryOperator != null) {
            operator = context.transform(binaryOperator, Operator.class);
        }

        if (operator == null) {
            context.reportProblem("Cannot transform constraint: Operator is missing");
            return null;
        }

        Expression leftExpression = null;
        if (constraint instanceof IdsConstraintImpl) {
            var leftOperand = ((IdsConstraintImpl) constraint).getLeftOperandAsString();
            if (leftOperand != null) {
                leftExpression = context.transform(leftOperand, Expression.class);
            }
        }

        if (leftExpression == null) {
            context.reportProblem("Cannot transform constraint: LeftExpression is missing");
            return null;
        }


        Expression rightExpression = null;
        var rightOperand = constraint.getRightOperand();
        if (rightOperand != null) {
            rightExpression = context.transform(rightOperand, Expression.class);
        }

        if (rightExpression == null) {
            context.reportProblem("Cannot transform constraint: RightExpression is missing");
            return null;
        }

        return AtomicConstraint.Builder.newInstance()
                .leftExpression(leftExpression)
                .operator(operator)
                .rightExpression(rightExpression)
                .build();
    }

}
