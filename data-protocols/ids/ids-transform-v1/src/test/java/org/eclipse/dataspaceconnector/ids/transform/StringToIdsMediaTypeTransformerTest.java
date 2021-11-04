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

class StringToIdsMediaTypeTransformerTest {
    private static final String STRING = "hello";

    // subject
    private StringToIdsMediaTypeTransformer stringToIdsMediaTypeTransformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        stringToIdsMediaTypeTransformer = new StringToIdsMediaTypeTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToIdsMediaTypeTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToIdsMediaTypeTransformer.transform(STRING, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = stringToIdsMediaTypeTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // record
        EasyMock.replay(context);

        // invoke
        var result = stringToIdsMediaTypeTransformer.transform(STRING, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(STRING, result.getFilenameExtension());
    }
}
