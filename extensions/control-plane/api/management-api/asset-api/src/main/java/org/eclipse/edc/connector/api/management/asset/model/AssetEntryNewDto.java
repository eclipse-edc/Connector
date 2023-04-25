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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.json.JsonObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonDeserialize(builder = AssetEntryNewDto.Builder.class)
public class AssetEntryNewDto {
    @NotNull(message = "asset cannot be null")
    private JsonObject asset;

    @NotNull(message = "dataAddress cannot be null")
    @Valid
    private DataAddressDto dataAddress;

    public DataAddressDto getDataAddress() {
        return dataAddress;
    }

    @Schema(implementation = Object.class, description = "A JSON structure in JSON-LD format containing the Asset's properties. Cannot be null.")
    public JsonObject getAsset() {
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

        public AssetEntryNewDto.Builder asset(JsonObject asset) {
            assetEntryDto.asset = asset;
            return this;
        }

        public AssetEntryNewDto.Builder dataAddress(DataAddressDto dataAddress) {
            assetEntryDto.dataAddress = dataAddress;
            return this;
        }

        public AssetEntryNewDto build() {
            return assetEntryDto;
        }
    }
}
