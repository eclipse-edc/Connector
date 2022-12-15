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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.eclipse.edc.protocol.ids.transform.type.asset.AssetToIdsResourceTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
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

    private static final String RESOURCE_ID = "1";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    private AssetToIdsResourceTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        context = mock(TransformerContext.class);
        transformer = new AssetToIdsResourceTransformer();
    }

    @Test
    void testSuccessfulSimple() {
        String description = "foo bar";
        var asset = Asset.Builder.newInstance().id(RESOURCE_ID).description(description).build();
        var representation = new RepresentationBuilder().build();
        when(context.transform(any(Asset.class), eq(Representation.class))).thenReturn(representation);

        var result = transformer.transform(asset, context);

        assertNotNull(result);
        assertEquals(RESOURCE_ID_URI, result.getId());
        assertEquals(1, result.getRepresentation().size());
        assertEquals(representation, result.getRepresentation().get(0));
        assertEquals(description, result.getDescription().get(0).getValue());
        verify(context, times(1)).transform(any(), any());
    }

}
