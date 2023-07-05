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
import org.eclipse.edc.api.model.BaseResponseDto;

import java.util.Map;

@JsonDeserialize(builder = AssetResponseDto.Builder.class)
@Deprecated(since = "0.1.3")
public class AssetResponseDto extends BaseResponseDto {

    private Map<String, Object> properties;

    private Map<String, Object> privateProperties;

    private AssetResponseDto() {
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<AssetResponseDto, Builder> {

        private Builder() {
            super(new AssetResponseDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, Object> properties) {
            dto.properties = properties;
            return this;
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            dto.privateProperties = privateProperties;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

    }
}
