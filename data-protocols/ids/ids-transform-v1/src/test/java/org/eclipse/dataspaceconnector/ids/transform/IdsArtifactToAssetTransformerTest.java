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
import de.fraunhofer.iais.eis.ArtifactImpl;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private IdsArtifactToAssetTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsArtifactToAssetTransformer();
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
            transformer.transform(new ArtifactBuilder().build(), null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        var artifact = new ArtifactBuilder(ARTIFACT_URI)
                ._fileName_(ASSET_FILENAME)
                ._byteSize_(ASSET_BYTESIZE)
                .build();

        var result = transformer.transform(artifact, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(ASSET_ID, result.getId());
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_BYTE_SIZE), ASSET_BYTESIZE);
        Assertions.assertEquals(result.getProperties().get(TransformKeys.KEY_ASSET_FILE_NAME), ASSET_FILENAME);
    }

}
