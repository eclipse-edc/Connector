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

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringToUriTransformerTest {
    private static final String STRING = "https//example.com";

    // subject
    private StringToUriTransformer stringToUriTransformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        stringToUriTransformer = new StringToUriTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToUriTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToUriTransformer.transform(STRING, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = stringToUriTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // record
        EasyMock.replay(context);

        // invoke
        var result = stringToUriTransformer.transform(STRING, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(STRING, result.toString());
    }
}
