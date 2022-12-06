/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.asset.transform;

import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssetToAssetResponseDtoTransformerTest {

    private final AssetToAssetResponseDtoTransformer transformer = new AssetToAssetResponseDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var asset = Asset.Builder.newInstance().properties(Map.of("key", "value")).build();

        var assetDto = transformer.transform(asset, context);

        assertThat(assetDto).isNotNull();
        assertThat(assetDto.getProperties()).containsExactlyEntriesOf(asset.getProperties());
        assertThat(assetDto.getCreatedAt()).isEqualTo(asset.getCreatedAt());
        assertThat(assetDto.getId()).isEqualTo(asset.getId());
    }

    @Test
    void transform_nullInput() {
        var context = mock(TransformerContext.class);

        var asset = transformer.transform(null, context);

        assertThat(asset).isNull();
        verify(context).reportProblem("input asset is null");
    }
}
