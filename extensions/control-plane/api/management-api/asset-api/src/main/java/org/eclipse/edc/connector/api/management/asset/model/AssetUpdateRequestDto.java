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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.AssertTrue;

import java.util.Map;

@JsonDeserialize(builder = AssetUpdateRequestDto.Builder.class)
public class AssetUpdateRequestDto extends AssetRequestDto {


    private AssetUpdateRequestDto() {
    }

    @JsonIgnore
    @AssertTrue(message = "no empty property keys and no duplicate keys")
    public boolean isValid() {
        return mapKeysValid();
    }

    @JsonIgnore
    @AssertTrue(message = "no duplicate keys in properties and private properties")
    public boolean containsDistinctKeys() {
        return checkDistinctKeys();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AssetRequestDto.Builder<AssetUpdateRequestDto, Builder> {

        private Builder() {
            super(new AssetUpdateRequestDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AssetUpdateRequestDto build() {
            return dto;
        }
    }
}
