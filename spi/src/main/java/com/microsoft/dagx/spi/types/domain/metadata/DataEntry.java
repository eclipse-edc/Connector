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
 *
 * @param <T> domain-specific extension properties.
 */
@JsonDeserialize(builder = DataEntry.Builder.class)
public class DataEntry<T extends DataCatalog> {
    private String id;
    private String policyId;
    private T catalog;

    private DataEntry() {
    }

    public String getId() {
        return id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public T getCatalog() {
        return catalog;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<K extends DataCatalog> {
        private final DataEntry<K> dataEntry;

        private Builder() {
            dataEntry = new DataEntry<>();

        }

        @JsonCreator
        public static <K extends DataCatalog> Builder<K> newInstance() {
            return new Builder<>();
        }

        public Builder<K> catalog(K extensions) {
            dataEntry.catalog = extensions;
            return this;
        }

        public Builder<K> id(String id) {
            dataEntry.id = id;
            return this;
        }

        public Builder<K> policyId(String id) {
            dataEntry.policyId = id;
            return this;
        }

        public DataEntry<K> build() {
            return dataEntry;
        }
    }

}
