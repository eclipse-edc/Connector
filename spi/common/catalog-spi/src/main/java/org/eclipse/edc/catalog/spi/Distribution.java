/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = Distribution.Builder.class)
public class Distribution {

    private String format; //e.g. ids:s3+push
    private DataService dataService;
    private String dataServiceId;

    public String getFormat() {
        return format;
    }

    @JsonIgnore
    public DataService getDataService() {
        return dataService;
    }
    
    @JsonProperty("dataService")
    public String getDataServiceId() {
        return dataServiceId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Distribution distribution;

        private Builder() {
            distribution = new Distribution();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder format(String format) {
            distribution.format = format;
            return this;
        }

        public Builder dataService(DataService dataService) {
            distribution.dataService = dataService;
            return this;
        }

        public Builder dataServiceId(String dataServiceId) {
            distribution.dataServiceId = dataServiceId;
            return this;
        }

        public Distribution build() {
            if (distribution.dataServiceId == null) {
                Objects.requireNonNull(distribution.dataService, "DataService must not be null.");
            }

            Objects.requireNonNull(distribution.format, "Format must not be null.");

            return distribution;
        }
    }

}
