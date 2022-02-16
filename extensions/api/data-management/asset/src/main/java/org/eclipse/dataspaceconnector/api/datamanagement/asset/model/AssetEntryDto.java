/*
 * Copyright (c) 2022 Diego Gomez
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Diego Gomez - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AssetEntryDto.Builder.class)
public class AssetEntryDto {

    private AssetDto assetDto;
    private DataAddress dataAddress;

    private AssetEntryDto(){
    }

    public AssetDto getAssetDto() {
        return assetDto;
    }

    public void setAssetDto(AssetDto assetDto) {
        this.assetDto = assetDto;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public void setDataAddress(DataAddress dataAddress) {
        this.dataAddress = dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder{

        private final AssetEntryDto assetEntryDto;

        private Builder(){

            assetEntryDto = new AssetEntryDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetDto(AssetDto assetDto){
            assetEntryDto.assetDto = assetDto;
            return this;
        }

        public Builder dataAddress(DataAddress dataAddress){
            assetEntryDto.dataAddress=dataAddress;
            return this;
        }

        public AssetEntryDto build(){
            return assetEntryDto;
        }
    }
}
