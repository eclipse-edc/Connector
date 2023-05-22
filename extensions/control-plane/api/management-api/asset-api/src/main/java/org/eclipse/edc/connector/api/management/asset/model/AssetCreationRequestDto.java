/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import java.util.Optional;

@JsonDeserialize(builder = AssetCreationRequestDto.Builder.class)
public class AssetCreationRequestDto extends AssetRequestDto {

    private String id;

    private AssetCreationRequestDto() {
    }

    @JsonIgnore
    @AssertTrue(message = "no empty property keys and no duplicate keys")
    public boolean isValid() {
        return mapKeysValid();
    }

    @JsonIgnore
    @AssertTrue(message = "no duplicate keys in properties and private properties")
    public boolean isDistinctKeysValid() {
        return checkDistinctKeys();
    }

    @JsonIgnore
    @AssertTrue(message = "id must be either null or not blank")
    public boolean isIdValid() {
        return Optional.of(this)
                .map(it -> it.id)
                .map(it -> !id.isBlank())
                .orElse(true);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends AssetRequestDto.Builder<AssetCreationRequestDto, Builder> {

        private Builder() {
            super(new AssetCreationRequestDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AssetCreationRequestDto build() {
            return dto;
        }
    }
}
