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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class IdsConstraintToConstraintTransformerTest {

    private static final URI CONSTRAINT_ID = URI.create("https://constraint.com");

    // subject
    private IdsConstraintToConstraintTransformer transformer;

    // mocks
    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsConstraintToConstraintTransformer();
        idsConstraint = new de.fraunhofer.iais.eis.ConstraintBuilder(CONSTRAINT_ID)
                ._leftOperand_(LeftOperand.PURPOSE)
                ._operator_(BinaryOperator.EQ)
                ._rightOperand_(new RdfResource("hello"))
                .build();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(idsConstraint, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testTransform() {
        // prepare
        var expectedLeftExpression = new LiteralExpression("left");
        var expectedRightExpression = new LiteralExpression("right");

        EasyMock.expect(context.transform(EasyMock.anyObject(LeftOperand.class), EasyMock.eq(Expression.class)))
                .andReturn(expectedLeftExpression);
        EasyMock.expect(context.transform(EasyMock.anyObject(BinaryOperator.class), EasyMock.eq(Operator.class)))
                .andReturn(Operator.EQ);
        EasyMock.expect(context.transform(EasyMock.anyObject(RdfResource.class), EasyMock.eq(Expression.class)))
                .andReturn(expectedRightExpression);

        // record
        EasyMock.replay(context);


        // invoke
        var result = (AtomicConstraint) transformer.transform(idsConstraint, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getLeftExpression());
        Assertions.assertEquals(result.getLeftExpression(), expectedLeftExpression);
        Assertions.assertNotNull(result.getOperator());
        Assertions.assertNotNull(result.getRightExpression());
        Assertions.assertEquals(result.getRightExpression(), expectedRightExpression);
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(context);
    }
}
