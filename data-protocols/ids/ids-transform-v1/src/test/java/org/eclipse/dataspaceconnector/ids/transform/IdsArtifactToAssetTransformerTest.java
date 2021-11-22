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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;

/**
 * Transforms an IDS Artifact into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation & Artifact,
 * there will be some kind of information loss.
 */
public class IdsArtifactToAssetTransformerTest {
    private static final String ASSET_ID = "1";
    private static final URI ARTIFACT_URI = URI.create("urn:artifact:1");
    private static final String ASSET_FILENAME = "test_filename";
    private static final BigInteger ASSET_BYTESIZE = BigInteger.valueOf(5);

    // subject
    private IdsArtifactToAssetTransformer transformer;

    // mocks
    private Artifact artifact;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsArtifactToAssetTransformer();
        artifact = EasyMock.createMock(Artifact.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(artifact, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(artifact, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(artifact, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(artifact, context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        EasyMock.expect(artifact.getId()).andReturn(ARTIFACT_URI);
        EasyMock.expect(artifact.getFileName()).andReturn(ASSET_FILENAME);
        EasyMock.expect(artifact.getByteSize()).andReturn(ASSET_BYTESIZE);

        // record
        EasyMock.replay(artifact, context);

        // invoke
        var result = transformer.transform(artifact, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ASSET_ID, result.getId());
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_BYTE_SIZE), ASSET_BYTESIZE);
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_FILE_NAME), ASSET_FILENAME);
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(artifact, context);
    }

}
