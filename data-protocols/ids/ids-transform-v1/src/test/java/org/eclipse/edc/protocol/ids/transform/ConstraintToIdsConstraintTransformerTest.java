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

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintImpl;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.transform.type.policy.ConstraintToIdsConstraintTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintToIdsConstraintTransformerTest {

    private ConstraintToIdsConstraintTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ConstraintToIdsConstraintTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testNonAtomicConstraint() {
        Constraint nonAtomicConstraint = mock(Constraint.class);

        var result = transformer.transform(nonAtomicConstraint, context);

        Assertions.assertNull(result);
        verify(context).reportProblem(anyString());
    }

    @Test
    void testSuccessfulSimple() {
        String leftOperand = "PURPOSE";
        RdfResource rightOperand = mock(RdfResource.class);
        BinaryOperator binaryOperator = BinaryOperator.AFTER;

        Expression leftExpression = mock(Expression.class);
        Expression rightExpression = mock(Expression.class);
        Operator operator = Operator.EQ;

        var constraint = AtomicConstraint.Builder.newInstance()
                .operator(operator)
                .leftExpression(leftExpression)
                .rightExpression(rightExpression)
                .build();

        var idsId = IdsId.Builder.newInstance().value(constraint.hashCode()).type(IdsType.CONSTRAINT).build();
        when(context.transform(eq(leftExpression), eq(String.class))).thenReturn(leftOperand);
        when(context.transform(eq(rightExpression), eq(RdfResource.class))).thenReturn(rightOperand);
        when(context.transform(eq(operator), eq(BinaryOperator.class))).thenReturn(binaryOperator);

        var result = (IdsConstraintImpl) transformer.transform(constraint, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(idsId.toUri(), result.getId());
        Assertions.assertEquals(leftOperand, result.getLeftOperandAsString());
        Assertions.assertEquals(rightOperand, result.getRightOperand());
        Assertions.assertEquals(binaryOperator, result.getOperator());
        verify(context).transform(eq(leftExpression), eq(String.class));
        verify(context).transform(eq(rightExpression), eq(RdfResource.class));
        verify(context).transform(eq(operator), eq(BinaryOperator.class));
    }
}
