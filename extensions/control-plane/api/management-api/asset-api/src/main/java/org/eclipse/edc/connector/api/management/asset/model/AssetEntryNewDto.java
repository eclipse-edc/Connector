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

package org.eclipse.edc.connector.api.management.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = AssetEntryNewDto.Builder.class)
@Deprecated(since = "0.1.3")
public class AssetEntryNewDto {

    public static final String EDC_ASSET_ENTRY_DTO_TYPE = EDC_NAMESPACE + "AssetEntryDto";
    public static final String EDC_ASSET_ENTRY_DTO_ASSET = EDC_NAMESPACE + "asset";
    public static final String EDC_ASSET_ENTRY_DTO_DATA_ADDRESS = EDC_NAMESPACE + "dataAddress";

    private Asset asset;
    private DataAddress dataAddress;

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public Asset getAsset() {
        return asset;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final AssetEntryNewDto assetEntryDto;

        private Builder() {
            assetEntryDto = new AssetEntryNewDto();
        }

        @JsonCreator
        public static AssetEntryNewDto.Builder newInstance() {
            return new AssetEntryNewDto.Builder();
        }

        public AssetEntryNewDto.Builder asset(Asset asset) {
            assetEntryDto.asset = asset;
            return this;
        }

        public AssetEntryNewDto.Builder dataAddress(DataAddress dataAddress) {
            assetEntryDto.dataAddress = dataAddress;
            return this;
        }

        public AssetEntryNewDto build() {
            return assetEntryDto;
        }
    }
}
