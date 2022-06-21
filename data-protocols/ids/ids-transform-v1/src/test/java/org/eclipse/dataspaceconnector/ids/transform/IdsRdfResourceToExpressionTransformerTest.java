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

import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class IdsRdfResourceToExpressionTransformerTest {
    private static final String VALUE = "COUNT";
    private RdfResource rdfResource;

    private IdsRdfResourceToExpressionTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsRdfResourceToExpressionTransformer();
        rdfResource = new RdfResource(VALUE);
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
            transformer.transform(rdfResource, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        var result = transformer.transform(rdfResource, context);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof LiteralExpression);
        Assertions.assertEquals(((LiteralExpression) result).getValue(), VALUE);
    }

}