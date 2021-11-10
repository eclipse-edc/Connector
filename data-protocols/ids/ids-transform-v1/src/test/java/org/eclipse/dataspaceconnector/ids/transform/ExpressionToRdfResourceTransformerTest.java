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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpressionToRdfResourceTransformerTest {

    // subject
    private ExpressionToRdfResourceTransformer transformer;

    // mocks
    private LiteralExpression expression;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ExpressionToRdfResourceTransformer();
        expression = EasyMock.createMock(LiteralExpression.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(expression, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(expression, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(expression, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(expression, context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        var value = "COUNT";
        EasyMock.expect(expression.getValue()).andReturn(value);

        // record
        EasyMock.replay(expression, context);

        // invoke
        var result = transformer.transform(expression, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(value, result.getValue());
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(expression, context);
    }

}
