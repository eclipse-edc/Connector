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

import de.fraunhofer.iais.eis.Action;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IdsActionToActionTransformTest {

    // subject
    private IdsActionToActionTransform transformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsActionToActionTransform();
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
            transformer.transform(Action.USE, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        EasyMock.replay(context);
        // invoke
        var result = transformer.transform(Action.USE, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals("USE", result.getType());
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(context);
    }
}
