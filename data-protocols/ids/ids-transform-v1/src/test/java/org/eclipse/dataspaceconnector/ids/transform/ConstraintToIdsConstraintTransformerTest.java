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

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintToIdsConstraintTransformerTest {

    private static final URI CONSTRAINT_ID = URI.create("https://constraint.com");

    private ConstraintToIdsConstraintTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ConstraintToIdsConstraintTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(AtomicConstraint.Builder.newInstance().build(), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
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
        LeftOperand leftOperand = LeftOperand.PURPOSE;
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
        when(context.transform(eq(idsId), eq(URI.class))).thenReturn(CONSTRAINT_ID);
        when(context.transform(eq(leftExpression), eq(LeftOperand.class))).thenReturn(leftOperand);
        when(context.transform(eq(rightExpression), eq(RdfResource.class))).thenReturn(rightOperand);
        when(context.transform(eq(operator), eq(BinaryOperator.class))).thenReturn(binaryOperator);

        var result = transformer.transform(constraint, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(CONSTRAINT_ID, result.getId());
        Assertions.assertEquals(leftOperand, result.getLeftOperand());
        Assertions.assertEquals(rightOperand, result.getRightOperand());
        Assertions.assertEquals(binaryOperator, result.getOperator());
        verify(context).transform(eq(idsId), eq(URI.class));
        verify(context).transform(eq(leftExpression), eq(LeftOperand.class));
        verify(context).transform(eq(rightExpression), eq(RdfResource.class));
        verify(context).transform(eq(operator), eq(BinaryOperator.class));
    }
}
