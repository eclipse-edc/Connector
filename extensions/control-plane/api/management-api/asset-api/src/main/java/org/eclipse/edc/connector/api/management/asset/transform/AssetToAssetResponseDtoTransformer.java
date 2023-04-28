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

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.asset.model.AssetResponseDto;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssetToAssetResponseDtoTransformer implements DtoTransformer<Asset, AssetResponseDto> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<AssetResponseDto> getOutputType() {
        return AssetResponseDto.class;
    }

    @Override
    public @Nullable AssetResponseDto transform(@NotNull Asset object, @NotNull TransformerContext context) {
        return AssetResponseDto.Builder.newInstance()
                .id(object.getId())
                .properties(object.getProperties())
                .privateProperties(object.getPrivateProperties())
                .createdAt(object.getCreatedAt())
                .build();
    }
}
