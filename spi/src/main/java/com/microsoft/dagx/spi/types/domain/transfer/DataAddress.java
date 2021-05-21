/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An address such as a data source or destination.
 */
@JsonDeserialize(builder = DataAddress.Builder.class)
public class DataAddress {
    private static final String TYPE = "type";
    private static final String KEYNAME = "keyName";
    private final Map<String, String> properties = new HashMap<>();

    private DataAddress() {
    }

    @NotNull
    public String getType() {
        return properties.get(TYPE);
    }

    public void setType(String type) {
        properties.replace(TYPE, type);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getKeyName() {
        return properties.get(KEYNAME);
    }

    public void setKeyName(String keyName) {
        properties.replace(KEYNAME, keyName);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataAddress address;

        private Builder() {
            address = new DataAddress();
        }

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(String type) {
            address.properties.put(TYPE, Objects.requireNonNull(type));
            return this;
        }

        public Builder property(String key, String value) {
            address.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            address.properties.putAll(properties);
            return this;
        }

        public Builder keyName(String keyName) {
            address.getProperties().put(KEYNAME, Objects.requireNonNull(keyName));
            return this;
        }

        public DataAddress build() {
            Objects.requireNonNull(address.getType(), "type");
//            Objects.requireNonNull(address.getKeyName(), "keyName");
            return address;
        }
    }
}
