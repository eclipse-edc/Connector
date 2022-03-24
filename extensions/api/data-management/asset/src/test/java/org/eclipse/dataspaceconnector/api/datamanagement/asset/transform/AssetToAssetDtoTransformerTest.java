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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssetToAssetDtoTransformerTest {

    private final AssetToAssetDtoTransformer transformer = new AssetToAssetDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void canHandle() {
        assertThat(transformer.canHandle(Asset.Builder.newInstance().build(), AssetDto.class)).isTrue();
        assertThat(transformer.canHandle("any other", AssetDto.class)).isFalse();
        assertThat(transformer.canHandle(Asset.Builder.newInstance().build(), Object.class)).isFalse();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var asset = Asset.Builder.newInstance().properties(Map.of("key", "value")).build();

        var assetDto = transformer.transform(asset, context);

        assertThat(assetDto.getProperties()).containsExactlyEntriesOf(asset.getProperties());
    }
}