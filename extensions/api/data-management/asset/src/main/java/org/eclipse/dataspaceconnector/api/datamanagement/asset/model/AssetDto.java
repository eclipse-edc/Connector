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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.eclipse.dataspaceconnector.api.model.BaseDto;

import java.util.Map;

@JsonDeserialize(builder = AssetDto.Builder.class)
public class AssetDto extends BaseDto {

    @NotNull(message = "properties cannot be null")
    private Map<String, Object> properties;

    @NotNull(message = "id cannot be null! (asset:prop:id is deprecated)")
    private String id;

    private AssetDto() {
    }

    @JsonIgnore
    @AssertTrue(message = "no empty property keys")
    public boolean isValid() {
        return properties != null && properties.keySet().stream().noneMatch(it -> it == null || it.isBlank());
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseDto.Builder<AssetDto, Builder> {

        private Builder() {
            super(new AssetDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, Object> properties) {
            dto.properties = properties;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }
    }
}
