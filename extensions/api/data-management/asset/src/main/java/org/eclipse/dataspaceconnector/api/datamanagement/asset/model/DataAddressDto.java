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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = DataAddressDto.Builder.class)
public class DataAddressDto {

    private Map<String, String> properties;

    private DataAddressDto() {
    }

    public Map<String, String> getProperties() {
        return properties;
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
