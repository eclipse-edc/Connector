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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An address that can be used resolve a data location. Data addresses are used throughout the system. For example, an asset has a data address used to resolve its contents,
 * which may be in an external store. A data address can also be used as a destination to send data during a transfer.
 * <p>
 * This type is extensible as different properties may be required to resolve data. For example, an HTTP data address will require a URL. Data addresses may also contain
 * references to information required to access the address, for example, the name of a shared token. Note, however, secrets should never be stored as properties as they may be
 * compromised if the data address is serialized.
 */
@JsonDeserialize(builder = DataAddress.Builder.class)
public class DataAddress {
    public static final String TYPE = "type";
    public static final String KEY_NAME = "keyName";
    private final Map<String, String> properties = new HashMap<>();

    protected DataAddress() {
    }

    @NotNull
    public String getType() {
        return properties.get(TYPE);
    }

    @JsonIgnore
    public void setType(String type) {
        Objects.requireNonNull(type);
        properties.put(TYPE, type);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getKeyName() {
        return properties.get(KEY_NAME);
    }

    @JsonIgnore
    public void setKeyName(String keyName) {
        Objects.requireNonNull(keyName);
        properties.put(KEY_NAME, keyName);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        protected final DataAddress address;

        protected Builder(DataAddress address) {
            this.address = address;
        }

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder(new DataAddress());
        }

        public Builder type(String type) {
            address.properties.put(TYPE, Objects.requireNonNull(type));
            return this;
        }

        public Builder property(String key, String value) {
            Objects.requireNonNull(key, "Property key null.");
            address.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            properties.forEach(this::property);
            return this;
        }

        public Builder keyName(String keyName) {
            address.getProperties().put(KEY_NAME, Objects.requireNonNull(keyName));
            return this;
        }

        public DataAddress build() {
            Objects.requireNonNull(address.getType(), "DataAddress builder missing Type property.");
            return address;
        }
    }
}
