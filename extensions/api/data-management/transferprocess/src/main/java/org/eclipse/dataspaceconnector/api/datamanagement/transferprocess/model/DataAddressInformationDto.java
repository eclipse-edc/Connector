/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = DataAddressInformationDto.Builder.class)
public class DataAddressInformationDto {

    private Map<String, String> properties;

    private DataAddressInformationDto() {
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final DataAddressInformationDto dataAddressDto;

        private Builder() {
            dataAddressDto = new DataAddressInformationDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, String> properties) {
            dataAddressDto.properties = properties;
            return this;
        }

        public DataAddressInformationDto build() {
            return dataAddressDto;
        }

    }
}
