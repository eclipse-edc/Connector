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

import de.fraunhofer.iais.eis.ArtifactBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Transforms an IDS Artifact into an {@link Asset}.
 * Please note that, as an {@link Asset} consists of an IDS Resource, Representation & Artifact,
 * there will be some kind of information loss.
 */
class IdsArtifactToAssetTransformerTest {
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
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> transformer.transform(null, null));
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> transformer.transform(new ArtifactBuilder().build(), null));
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        assertThat(result).isNull();
    }

    @Test
    void testSuccessfulMap() {
        var artifact = new ArtifactBuilder(ARTIFACT_URI)
                ._fileName_(ASSET_FILENAME)
                ._byteSize_(ASSET_BYTESIZE)
                .build();

        var result = transformer.transform(artifact, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ASSET_ID);
        assertThat(result.getProperties())
                .isNotNull()
                .containsEntry(TransformKeys.KEY_ASSET_BYTE_SIZE, ASSET_BYTESIZE)
                .containsEntry(TransformKeys.KEY_ASSET_FILE_NAME, ASSET_FILENAME);
    }

    @Test
    void testCustomProperties() {
        var artifact = new ArtifactBuilder(ARTIFACT_URI)
                ._fileName_(ASSET_FILENAME)
                ._byteSize_(ASSET_BYTESIZE)
                .build();

        artifact.setProperty("key1", "val1");
        artifact.setProperty("key2", "val2");

        var asset = transformer.transform(artifact, context);

        assertThat(asset).isNotNull();

        assertThat(asset.getProperties())
                .isNotNull()
                .containsEntry(TransformKeys.KEY_ASSET_BYTE_SIZE, ASSET_BYTESIZE)
                .containsEntry(TransformKeys.KEY_ASSET_FILE_NAME, ASSET_FILENAME)
                .containsEntry("key1", "val1")
                .containsEntry("key2", "val2");
    }

}
