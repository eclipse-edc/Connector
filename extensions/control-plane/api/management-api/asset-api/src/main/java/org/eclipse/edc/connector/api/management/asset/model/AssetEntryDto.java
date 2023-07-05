/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.DataAddressDto;

@JsonDeserialize(builder = AssetEntryDto.Builder.class)
@Deprecated(since = "0.1.3")
public class AssetEntryDto {
    private AssetCreationRequestDto asset;
    private DataAddressDto dataAddress;

    private AssetEntryDto() {
    }

    public AssetCreationRequestDto getAsset() {
        return asset;
    }

    public DataAddressDto getDataAddress() {
        return dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final AssetEntryDto assetEntryDto;

        private Builder() {
            assetEntryDto = new AssetEntryDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder asset(AssetCreationRequestDto asset) {
            assetEntryDto.asset = asset;
            return this;
        }

        public Builder dataAddress(DataAddressDto dataAddress) {
            assetEntryDto.dataAddress = dataAddress;
            return this;
        }

        public AssetEntryDto build() {
            return assetEntryDto;
        }
    }
}
