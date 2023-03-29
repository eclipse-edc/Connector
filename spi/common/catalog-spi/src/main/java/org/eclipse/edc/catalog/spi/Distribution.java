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

/**
 * Models the Distribution class of the DCAT spec. A Distribution is defined as a specific
 * representation of a Dataset. A Distribution contains a reference to the DataService, and thereby
 * endpoint, via which the Distribution can be obtained.
 */
@JsonDeserialize(builder = Distribution.Builder.class)
public class Distribution {
    
    /** Protocol/technology via which this Distribution is available. */
    private String format;
    
    /** DataService that contains access information for this Distribution. */
    private DataService dataService;
    
    /** Reference to the DataService. When mapping to DCAT, only the DataService ID is referenced. */
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
