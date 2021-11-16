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
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IdsConstraintToConstraintTransformer implements IdsTypeTransformer<de.fraunhofer.iais.eis.Constraint, Constraint> {

    @Override
    public Class<de.fraunhofer.iais.eis.Constraint> getInputType() {
        return de.fraunhofer.iais.eis.Constraint.class;
    }

    @Override
    public Class<Constraint> getOutputType() {
        return Constraint.class;
    }

    @Override
    public @Nullable Constraint transform(de.fraunhofer.iais.eis.Constraint object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        Expression leftOperand = new LiteralExpression(object.getLeftOperand().name());
        Expression rightOperand = new LiteralExpression(object.getRightOperand().getValue());
        Operator operator;
        switch (object.getOperator()) {
            case EQUALS:
            case EQ:
                operator = Operator.EQ;
                break;
            case GT:
                operator = Operator.GT;
                break;
            case GTEQ:
                operator = Operator.GEQ;
                break;
            case LT:
                operator = Operator.LT;
                break;
            case LTEQ:
                operator = Operator.LEQ;
                break;
            case IN:
                operator = Operator.IN;
                break;
            default:
                context.reportProblem(String.format("cannot transform IDS operator %s", object.getOperator()));
                return null;
        }

        var constraintBuilder = AtomicConstraint.Builder.newInstance();

        constraintBuilder.leftExpression(leftOperand);
        constraintBuilder.rightExpression(rightOperand);
        constraintBuilder.operator(operator);

        return constraintBuilder.build();
    }
}
