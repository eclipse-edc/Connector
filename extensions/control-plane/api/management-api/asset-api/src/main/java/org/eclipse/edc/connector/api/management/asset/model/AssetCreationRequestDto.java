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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;

@JsonDeserialize(builder = AssetCreationRequestDto.Builder.class)
@Deprecated(since = "0.1.3")
public class AssetCreationRequestDto extends AssetRequestDto {

    @JsonProperty(value = ID)
    private String id;

    private AssetCreationRequestDto() {
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
