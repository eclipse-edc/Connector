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

@Deprecated(since = "0.1.3")
public class AssetUpdateRequestWrapperDto {

    private AssetUpdateRequestDto requestDto;
    private String assetId;

    private AssetUpdateRequestWrapperDto() {
    }

    public AssetUpdateRequestDto getRequestDto() {
        return requestDto;
    }

    public String getAssetId() {
        return assetId;
    }


    public static final class Builder {

        private final AssetUpdateRequestWrapperDto dto;

        private Builder() {
            dto = new AssetUpdateRequestWrapperDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder updateRequest(AssetUpdateRequestDto dto) {
            this.dto.requestDto = dto;
            return this;
        }

        public Builder assetId(String assetId) {
            this.dto.assetId = assetId;
            return this;
        }

        public AssetUpdateRequestWrapperDto build() {
            return dto;
        }
    }
}
