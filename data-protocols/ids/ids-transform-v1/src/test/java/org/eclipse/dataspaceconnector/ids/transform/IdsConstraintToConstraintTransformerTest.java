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
    private IdsConstraintToConstraintTransformer idsConstraintToConstraintTransformer;

    // mocks
    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        idsConstraintToConstraintTransformer = new IdsConstraintToConstraintTransformer();
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
            idsConstraintToConstraintTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            idsConstraintToConstraintTransformer.transform(idsConstraint, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = idsConstraintToConstraintTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        var leftExpression = new LiteralExpression(LeftOperand.PURPOSE.name());
        var rightExpression = new LiteralExpression("hello");
        var operator = Operator.EQ;

        // record
        EasyMock.replay(context);

        // invoke
        var result = (AtomicConstraint) idsConstraintToConstraintTransformer.transform(idsConstraint, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(leftExpression, result.getLeftExpression());
        Assertions.assertEquals(rightExpression, result.getRightExpression());
        Assertions.assertEquals(operator, result.getOperator());
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(context);
    }
}
