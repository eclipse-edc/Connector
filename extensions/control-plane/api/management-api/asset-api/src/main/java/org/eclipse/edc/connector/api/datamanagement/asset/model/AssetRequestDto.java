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

package org.eclipse.edc.connector.api.datamanagement.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Optional;

@JsonDeserialize(builder = AssetRequestDto.Builder.class)
public class AssetRequestDto {

    private String id;

    @NotNull(message = "properties cannot be null")
    private Map<String, Object> properties;

    private AssetRequestDto() {
    }

    @JsonIgnore
    @AssertTrue(message = "no empty property keys")
    public boolean isValid() {
        return properties != null && properties.keySet().stream().noneMatch(it -> it == null || it.isBlank());
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

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final AssetRequestDto dto;

        private Builder() {
            this.dto = new AssetRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            dto.properties = properties;
            return this;
        }

        public AssetRequestDto build() {
            return dto;
        }
    }
}
