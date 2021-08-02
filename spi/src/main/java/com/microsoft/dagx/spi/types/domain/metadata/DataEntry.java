/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Data that is managed and can be shared by the system.
 */
@JsonDeserialize(builder = DataEntry.Builder.class)
public class DataEntry {
    private String id;
    private String policyId;
    private DataCatalogEntry catalogEntry;

    private DataEntry() {
    }

    public String getId() {
        return id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public DataCatalogEntry getCatalogEntry() {
        return catalogEntry;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataEntry dataEntry;

        private Builder() {
            dataEntry = new DataEntry();

        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder catalogEntry(DataCatalogEntry extensions) {
            dataEntry.catalogEntry = extensions;
            return this;
        }

        public Builder id(String id) {
            dataEntry.id = id;
            return this;
        }

        public Builder policyId(String id) {
            dataEntry.policyId = id;
            return this;
        }

        public DataEntry build() {
            return dataEntry;
        }
    }

}
