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

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.Representation;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetToIdsRepresentationTransformerTest {
    private static final String REPRESENTATION_ID = "test_id";
    private static final URI REPRESENTATION_ID_URI = URI.create("urn:representation:1");
    private static final String ASSET_FILE_EXTENSION = "file_extension";

    // subject
    private AssetToIdsRepresentationTransformer transformer;

    // mocks
    private Asset asset;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new AssetToIdsRepresentationTransformer();
        asset = mock(Asset.class);
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
            transformer.transform(asset, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        when(asset.getId()).thenReturn(REPRESENTATION_ID);
        when(asset.getProperties()).thenReturn(Collections.emptyMap());

        var artifact = new ArtifactBuilder().build();
        when(context.transform(any(Asset.class), eq(Artifact.class))).thenReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(REPRESENTATION_ID_URI);

        var result = transformer.transform(asset, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
    }

    @Test
    void testSuccessfulMap() {
        when(asset.getId()).thenReturn(REPRESENTATION_ID);
        Map<String, Object> properties = new HashMap<>() {
            {
                put(TransformKeys.KEY_ASSET_FILE_EXTENSION, ASSET_FILE_EXTENSION);
            }
        };
        when(asset.getProperties()).thenReturn(properties);

        var artifact = new ArtifactBuilder().build();
        when(context.transform(any(Asset.class), eq(Artifact.class))).thenReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(REPRESENTATION_ID_URI);

        Representation result = transformer.transform(asset, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
        Assertions.assertEquals(ASSET_FILE_EXTENSION, result.getMediaType().getFilenameExtension());
    }
}
