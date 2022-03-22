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

import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.Mockito.mock;

class UriToIdsIdTransformerTest {

    private static final IdsType IDS_ID_TYPE = IdsType.ARTIFACT;
    private static final String IDS_ID_VALUE = "32d39d70-68f7-44f3-b8b2-27550f2081f4";
    private static final URI URI = java.net.URI.create("urn:artifact:32d39d70-68f7-44f3-b8b2-27550f2081f4");

    private UriToIdsIdTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    public void setup() {
        transformer = new UriToIdsIdTransformer();
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
            transformer.transform(URI, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        var result = transformer.transform(URI, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(IDS_ID_TYPE, result.getType());
        Assertions.assertEquals(IDS_ID_VALUE, result.getValue());
    }
}
