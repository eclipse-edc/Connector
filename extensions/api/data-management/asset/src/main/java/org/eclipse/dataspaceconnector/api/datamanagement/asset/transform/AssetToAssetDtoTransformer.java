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
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssetToAssetDtoTransformer implements DtoTransformer<Asset, AssetDto> {

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<AssetDto> getOutputType() {
        return AssetDto.class;
    }

    @Override
    public boolean canHandle(@NotNull Object object, @NotNull Class<?> outputType) {
        return getInputType().isInstance(object) && outputType.equals(getOutputType());
    }

    @Override
    public @Nullable AssetDto transform(@Nullable Asset object, @NotNull TransformerContext context) {
        return AssetDto.Builder.newInstance().properties(object.getProperties()).build();
    }
}
