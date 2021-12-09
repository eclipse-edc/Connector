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

import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpressionToIdsRdfResourceTransformerTest {

    private ExpressionToIdsRdfResourceTransformer transformer;

    private LiteralExpression expression;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ExpressionToIdsRdfResourceTransformer();
        expression = mock(LiteralExpression.class);
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
            transformer.transform(expression, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        var value = "COUNT";
        when(expression.asString()).thenReturn(value);

        var result = transformer.transform(expression, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(value, result.getValue());
    }

}
