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

import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetToIdsArtifactTransformerTest {
    private static final String ASSET_ID = "test_id";
    private static final URI ASSET_ID_URI = URI.create("urn:asset:1");
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
    void testThrowsNullPointerExceptionForAll() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> transformer.transform(null, null));
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> transformer.transform(Asset.Builder.newInstance().build(), null));
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        assertThat(result).isNull();
    }

    @Test
    void testSuccessfulSimple() {
        var asset = Asset.Builder.newInstance().id(ASSET_ID).build();

        IdsId id = IdsId.Builder.newInstance().value(ASSET_ID).type(IdsType.ARTIFACT).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(ASSET_ID_URI);

        var result = transformer.transform(asset, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ASSET_ID_URI);
        verify(context).transform(eq(id), eq(URI.class));
    }

    @Test
    void testSuccessfulMap() {
        var properties = new HashMap<>(Map.of(TransformKeys.KEY_ASSET_FILE_NAME, ASSET_FILENAME, TransformKeys.KEY_ASSET_BYTE_SIZE, ASSET_BYTESIZE));
        var asset = Asset.Builder.newInstance().properties(properties).id(ASSET_ID).build();

        IdsId id = IdsId.Builder.newInstance().value(ASSET_ID).type(IdsType.ARTIFACT).build();
        when(context.transform(eq(id), eq(URI.class))).thenReturn(ASSET_ID_URI);

        var result = transformer.transform(asset, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ASSET_ID_URI);
        assertThat(result.getFileName()).isEqualTo(ASSET_FILENAME);
        assertThat(result.getByteSize()).isEqualTo(ASSET_BYTESIZE);

        verify(context).transform(eq(id), eq(URI.class));
    }

}