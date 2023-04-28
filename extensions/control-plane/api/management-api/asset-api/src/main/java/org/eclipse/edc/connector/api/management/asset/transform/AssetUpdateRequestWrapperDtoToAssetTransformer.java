/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.connector.api.management.asset.model.AssetUpdateRequestWrapperDto;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssetUpdateRequestWrapperDtoToAssetTransformer implements DtoTransformer<AssetUpdateRequestWrapperDto, Asset> {
    @Override
    public Class<AssetUpdateRequestWrapperDto> getInputType() {
        return AssetUpdateRequestWrapperDto.class;
    }

    @Override
    public Class<Asset> getOutputType() {
        return Asset.class;
    }

    @Override
    public @Nullable Asset transform(@NotNull AssetUpdateRequestWrapperDto object, @NotNull TransformerContext context) {
        return Asset.Builder.newInstance()
                .properties(object.getRequestDto().getProperties())
                .privateProperties(object.getRequestDto().getPrivateProperties())
                .id(object.getAssetId())
                .build();
    }
}
