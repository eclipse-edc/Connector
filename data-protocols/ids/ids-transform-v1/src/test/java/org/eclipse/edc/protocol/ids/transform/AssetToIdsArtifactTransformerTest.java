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

import org.eclipse.edc.protocol.ids.transform.type.asset.AssetToIdsArtifactTransformer;
import org.eclipse.edc.protocol.ids.transform.type.asset.TransformKeys;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssetToIdsArtifactTransformerTest {
    private static final String ASSET_ID = "1";
    private static final URI ASSET_ID_URI = URI.create("urn:artifact:1");
    private static final String ASSET_FILENAME = "test_filename";
    private static final BigInteger ASSET_BYTESIZE = BigInteger.valueOf(5);

    private AssetToIdsArtifactTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new AssetToIdsArtifactTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void transformAssetWithProperties() {
        var id = "test-id";
        var asset = Asset.Builder.newInstance().id(id)
                .name("test-name")
                .version("1.0")
                .property("somekey", "somevalue")
                .build();

        var idsAsset = transformer.transform(asset, context);
        assertThat(idsAsset).isNotNull();

        assertThat(idsAsset.getProperties())
                .isNotNull()
                .containsEntry("somekey", "somevalue");
    }

    @Test
    void testSuccessfulSimple() {
        var asset = Asset.Builder.newInstance().id(ASSET_ID).build();

        var result = transformer.transform(asset, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ASSET_ID_URI);
    }

    @Test
    void testSuccessfulMap() {
        var properties = Map.<String, Object>of(TransformKeys.KEY_ASSET_FILE_NAME, ASSET_FILENAME, TransformKeys.KEY_ASSET_BYTE_SIZE, ASSET_BYTESIZE);
        var asset = Asset.Builder.newInstance().properties(properties).id(ASSET_ID).build();

        var result = transformer.transform(asset, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ASSET_ID_URI);
        assertThat(result.getFileName()).isEqualTo(ASSET_FILENAME);
        assertThat(result.getByteSize()).isEqualTo(ASSET_BYTESIZE);
    }

}
