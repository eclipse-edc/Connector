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
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionToActionTransformerTest {

    // subject
    private ActionToActionTransformer transformer;

    // mocks
    private Action action;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ActionToActionTransformer();
        action = EasyMock.createMock(Action.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(action, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(action, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(action, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(action, context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        EasyMock.expect(action.getType()).andReturn("USE");

        // record
        EasyMock.replay(action, context);

        // invoke
        var result = transformer.transform(action, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(de.fraunhofer.iais.eis.Action.USE, result);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(action, context);
    }
}
