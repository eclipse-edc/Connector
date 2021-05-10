/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic extension properties.
 */
@JsonTypeName("dagx:genericdataentryextensions")
@JsonDeserialize(builder = GenericDataCatalog.Builder.class)
public class GenericDataCatalog implements DataCatalog {
    private final Map<String, Object> properties = new HashMap<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public DataAddress getPropertiesForEntity(String id) {
        return DataAddress.Builder.newInstance()
                //convert a Map of String-Object to String-String
                .properties(getProperties().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toString())))
                .build();

    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final GenericDataCatalog lookup;

        private Builder() {
            lookup = new GenericDataCatalog();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder property(String key, String value) {
            lookup.properties.put(key, value);
            return this;
        }

        public GenericDataCatalog build() {
            return lookup;
        }
    }


}
