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
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdsIdToUriTransformerTest {

    private static final IdsType IDS_ID_TYPE = IdsType.ARTIFACT;
    private static final String IDS_ID_VALUE = "1c6865e0-80ca-4811-bcf0-fcad250b538f";

    // subject
    private IdsIdToUriTransformer idsIdToUriTransformer;

    // mocks
    private IdsId idsId;
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        idsIdToUriTransformer = new IdsIdToUriTransformer();
        idsId = EasyMock.createMock(IdsId.class);
        context = EasyMock.createMock(TransformerContext.class);
    }


    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(idsId, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            idsIdToUriTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(idsId, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            idsIdToUriTransformer.transform(idsId, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(idsId, context);

        var result = idsIdToUriTransformer.transform(null, context);

        Assertions.assertNull(result);
    }


    @Test
    void testSuccessfulSimple() {
        // prepare
        EasyMock.expect(idsId.getType()).andReturn(IDS_ID_TYPE);
        EasyMock.expect(idsId.getValue()).andReturn(IDS_ID_VALUE);

        // record
        EasyMock.replay(idsId, context);

        // invoke
        var result = idsIdToUriTransformer.transform(idsId, context);

        // verify
        Assertions.assertNotNull(result);
    }

}
