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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.Builder;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_ASSET;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_DATA_ADDRESS;

@Deprecated(since = "0.1.3")
public class JsonObjectToAssetEntryNewDtoTransformer extends AbstractJsonLdTransformer<JsonObject, AssetEntryNewDto> {

    public JsonObjectToAssetEntryNewDtoTransformer() {
        super(JsonObject.class, AssetEntryNewDto.class);
    }

    @Override
    public @Nullable AssetEntryNewDto transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = Builder.newInstance();

        visitProperties(input, key -> {
            switch (key) {
                case EDC_ASSET_ENTRY_DTO_ASSET:
                    return v -> transformArrayOrObject(v, Asset.class, builder::asset, context);
                case EDC_ASSET_ENTRY_DTO_DATA_ADDRESS:
                    return v -> transformArrayOrObject(v, DataAddress.class, builder::dataAddress, context);
                default:
                    return doNothing();
            }
        });

        return builder.build();
    }

}
