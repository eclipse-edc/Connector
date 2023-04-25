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
import org.eclipse.edc.api.model.BaseResponseDto;

@JsonDeserialize(builder = AssetResponseNewDto.Builder.class)
public class AssetResponseNewDto extends BaseResponseDto {
    private JsonObject asset;

    @Schema(implementation = Object.class, description = "A JSON structure in JSON-LD format containing the Asset's properties.")
    public JsonObject getAsset() {
        return asset;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends BaseResponseDto.Builder<AssetResponseNewDto, AssetResponseNewDto.Builder> {

        private Builder() {
            super(new AssetResponseNewDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder asset(JsonObject asset) {
            dto.asset = asset;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
