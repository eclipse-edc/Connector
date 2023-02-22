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
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@JsonDeserialize(builder = AssetUpdateDto.Builder.class)
public class AssetUpdateDto {


    @NotNull(message = "properties cannot be null")
    private Map<String, Object> properties;

    private AssetUpdateDto() {
    }

    @JsonIgnore
    @AssertTrue(message = "no empty property keys")
    public boolean isValid() {
        return properties != null && properties.keySet().stream().noneMatch(it -> it == null || it.isBlank());
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final AssetUpdateDto dto;

        private Builder() {
            this.dto = new AssetUpdateDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, Object> properties) {
            dto.properties = properties;
            return this;
        }

        public AssetUpdateDto build() {
            return dto;
        }
    }
}
