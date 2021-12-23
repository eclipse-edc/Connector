/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.types.domain;

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
        Objects.requireNonNull(type);
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
        Objects.requireNonNull(keyName);
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
            Objects.requireNonNull(value);
            address.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, ?> properties) {
            // ArtifactRequestMessageImpl#urifyObjects (line 176): this "feature" converts every string starting with
            // 'http' to become a URI.
            // Thus, sometimes the <String, String> map can contain ... you guessed it ... URIs :(
            properties.forEach((k, v) -> address.properties.put(k, v.toString()));
            return this;
        }

        public Builder keyName(String keyName) {
            address.getProperties().put(KEYNAME, keyName);
            return this;
        }

        public DataAddress build() {
            if (!address.getProperties().isEmpty()) {
                Objects.requireNonNull(address.getType(), "type");
            }
            return address;
        }
    }
}
