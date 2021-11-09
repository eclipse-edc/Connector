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
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AssetToIdsArtifactTransformerTest {
    private static final String ASSET_ID = "test_id";
    private static final URI ASSET_ID_URI = URI.create("urn:asset:1");
    private static final String ASSET_FILENAME = "test_filename";
    private static final BigInteger ASSET_BYTESIZE = BigInteger.valueOf(5);

    // subject
    private AssetToIdsArtifactTransformer assetToIdsArtifactTransformer;

    // mocks
    private Asset asset;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        assetToIdsArtifactTransformer = new AssetToIdsArtifactTransformer();
        asset = EasyMock.createMock(Asset.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToIdsArtifactTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToIdsArtifactTransformer.transform(asset, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(asset, context);

        var result = assetToIdsArtifactTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(ASSET_ID);
        EasyMock.expect(asset.getProperties()).andReturn(Collections.emptyMap());

        IdsId id = IdsId.Builder.newInstance().value(ASSET_ID).type(IdsType.ARTIFACT).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(ASSET_ID_URI);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(asset, context);

        // invoke
        var result = assetToIdsArtifactTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ASSET_ID_URI, result.getId());
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(ASSET_ID);
        Map<String, Object> properties = new HashMap<>() {
            {
                put(TransformKeys.KEY_ASSET_FILE_NAME, ASSET_FILENAME);
                put(TransformKeys.KEY_ASSET_BYTE_SIZE, ASSET_BYTESIZE);
            }
        };
        EasyMock.expect(asset.getProperties()).andReturn(properties);

        IdsId id = IdsId.Builder.newInstance().value(ASSET_ID).type(IdsType.ARTIFACT).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(ASSET_ID_URI);

        // record
        EasyMock.replay(asset, context);

        // invoke
        var result = assetToIdsArtifactTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ASSET_ID_URI, result.getId());
        Assertions.assertEquals(ASSET_FILENAME, result.getFileName());
        Assertions.assertEquals(ASSET_BYTESIZE, result.getByteSize());
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(asset, context);
    }

}