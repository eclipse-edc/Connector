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
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionToIdsActionTransformerTest {

    // subject
    private ActionToIdsActionTransformer transformer;

    // mocks
    private Action action;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ActionToIdsActionTransformer();
        action = mock(Action.class);
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
            transformer.transform(action, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        when(action.getType()).thenReturn("USE");

        var result = transformer.transform(action, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(de.fraunhofer.iais.eis.Action.USE, result);
    }
}
