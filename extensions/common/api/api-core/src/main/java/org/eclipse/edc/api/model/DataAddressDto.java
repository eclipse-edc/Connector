/*
 *  Copyright (c) 2022 - 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - initial API and implementation
 *
 */

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@JsonDeserialize(builder = DataAddressDto.Builder.class)
public class DataAddressDto extends BaseDto {

    @NotNull(message = "properties cannot be null")
    private Map<String, String> properties;

    private DataAddressDto() {
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonIgnore
    @AssertTrue(message = "property keys cannot be blank and property 'type' is mandatory")
    public boolean isValid() {
        return properties != null && properties.keySet().stream().noneMatch(it -> it == null || it.isBlank()) && properties.containsKey("type");
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final DataAddressDto dataAddressDto;

        private Builder() {
            dataAddressDto = new DataAddressDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, String> properties) {
            dataAddressDto.properties = properties;
            return this;
        }

        public DataAddressDto build() {
            return dataAddressDto;
        }

    }
}
