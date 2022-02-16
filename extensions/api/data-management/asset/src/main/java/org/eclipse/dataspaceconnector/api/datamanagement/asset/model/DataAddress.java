/*
 * Copyright (c) 2022 Diego Gomez
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Diego Gomez - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DataAddress.Builder.class)
public class DataAddress {

    private Map<String, Object> properties;

    private DataAddress() {
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final DataAddress dataAddress;

        private Builder() {
            dataAddress = new DataAddress();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder properties(Map<String, Object> properties) {
            dataAddress.properties = properties;
            return this;
        }

        public DataAddress build() {
            return dataAddress;
        }

    }
}
