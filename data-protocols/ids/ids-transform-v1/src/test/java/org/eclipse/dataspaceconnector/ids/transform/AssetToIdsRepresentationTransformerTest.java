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
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.MediaType;
import de.fraunhofer.iais.eis.Representation;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AssetToIdsRepresentationTransformerTest {
    private static final String REPRESENTATION_ID = "test_id";
    private static final URI REPRESENTATION_ID_URI = URI.create("urn:representation:1");
    private static final String ASSET_FILE_EXTENSION = "file_extension";
    private static final MediaType MEDIA_TYPE = new CustomMediaTypeBuilder()._filenameExtension_(ASSET_FILE_EXTENSION).build();

    // subject
    private AssetToIdsRepresentationTransformer assetToIdsRepresentationTransformer;

    // mocks
    private Asset asset;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        assetToIdsRepresentationTransformer = new AssetToIdsRepresentationTransformer();
        asset = EasyMock.createMock(Asset.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToIdsRepresentationTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToIdsRepresentationTransformer.transform(asset, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(asset, context);

        var result = assetToIdsRepresentationTransformer.transform(null, context);

        Assertions.assertNull(result);
    }


    @Test
    void testSuccessfulSimple() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(REPRESENTATION_ID);
        EasyMock.expect(asset.getProperties()).andReturn(Collections.emptyMap());

        var artifact = new ArtifactBuilder().build();
        EasyMock.expect(context.transform(EasyMock.anyObject(Asset.class), EasyMock.eq(Artifact.class))).andReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(REPRESENTATION_ID_URI);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(asset, context);

        // invoke
        var result = assetToIdsRepresentationTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(REPRESENTATION_ID);
        Map<String, Object> properties = new HashMap<>() {
            {
                put(TransformKeys.KEY_ASSET_FILE_EXTENSION, ASSET_FILE_EXTENSION);
            }
        };
        EasyMock.expect(asset.getProperties()).andReturn(properties);

        EasyMock.expect(context.transform(ASSET_FILE_EXTENSION, MediaType.class)).andReturn(MEDIA_TYPE);

        var artifact = new ArtifactBuilder().build();
        EasyMock.expect(context.transform(EasyMock.anyObject(Asset.class), EasyMock.eq(Artifact.class))).andReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(REPRESENTATION_ID_URI);

        // record
        EasyMock.replay(asset, context);

        // invoke
        Representation result = assetToIdsRepresentationTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
        Assertions.assertEquals(ASSET_FILE_EXTENSION, result.getMediaType().getFilenameExtension());
    }


    @AfterEach
    void tearDown() {
        EasyMock.verify(asset, context);
    }
}
