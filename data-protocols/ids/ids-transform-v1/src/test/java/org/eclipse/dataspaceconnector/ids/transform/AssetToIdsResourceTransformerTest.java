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

import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetToIdsResourceTransformerTest {

    private static final String RESOURCE_ID = "test_id";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    private AssetToIdsResourceTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        context = mock(TransformerContext.class);
        transformer = new AssetToIdsResourceTransformer();
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
            transformer.transform(Asset.Builder.newInstance().build(), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        String description = "foo bar";
        var asset = Asset.Builder.newInstance().id(RESOURCE_ID).description(description).build();
        var representation = new RepresentationBuilder().build();
        when(context.transform(any(Asset.class), eq(Representation.class))).thenReturn(representation);

        IdsId id = IdsId.Builder.newInstance().value(RESOURCE_ID).type(IdsType.RESOURCE).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(RESOURCE_ID_URI);

        var result = transformer.transform(asset, context);

        assertNotNull(result);
        assertEquals(RESOURCE_ID_URI, result.getId());
        assertEquals(1, result.getRepresentation().size());
        assertEquals(representation, result.getRepresentation().get(0));
        assertEquals(description, result.getDescription().get(0).getValue());
        verify(context, times(2)).transform(any(), any());
    }

}
