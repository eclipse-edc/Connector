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
 *       Siemens AG - enable read property and return a default value is missing
 *
 */

package org.eclipse.edc.spi.types.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;


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
    public static final String SIMPLE_TYPE = "type";
    public static final String TYPE = EDC_NAMESPACE + SIMPLE_TYPE;
    public static final String SIMPLE_KEY_NAME = "keyName";
    public static final String KEY_NAME = EDC_NAMESPACE + "keyName";
    public static final String SECRET = EDC_NAMESPACE + "secret";
    protected final Map<String, String> properties = new HashMap<>();

    protected DataAddress() {
    }

    @NotNull
    public String getType() {
        return getProperty(TYPE);
    }

    @JsonIgnore
    public void setType(String type) {
        Objects.requireNonNull(type);
        properties.put(TYPE, type);
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defaultValue) {
        var value = Optional.ofNullable(properties.get(EDC_NAMESPACE + key)).orElseGet(() -> properties.get(key));
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getKeyName() {
        return getProperty(KEY_NAME);
    }

    @JsonIgnore
    public void setKeyName(String keyName) {
        Objects.requireNonNull(keyName);
        properties.put(KEY_NAME, keyName);
    }

    /**
     * Returns true if there's a property with the specified key
     *
     * @param key the key
     * @return true if it exists, false if not
     */
    @JsonIgnore
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<DA extends DataAddress, B extends Builder<DA, B>> {
        protected final DA address;

        protected Builder(DA address) {
            this.address = address;
        }

        @JsonCreator()
        public static <B extends Builder<DataAddress, B>> Builder<DataAddress, B> newInstance() {
            return new Builder<>(new DataAddress());
        }

        public B type(String type) {
            address.properties.put(TYPE, Objects.requireNonNull(type));
            return self();
        }

        public B property(String key, String value) {
            Objects.requireNonNull(key, "Property key null.");
            if (SIMPLE_TYPE.equals(key)) {
                key = TYPE;
            } else if (SIMPLE_KEY_NAME.equals(key)) {
                key = KEY_NAME;
            }
            address.properties.put(key, value);
            return self();
        }

        public B properties(Map<String, String> properties) {
            properties.forEach(this::property);
            return self();
        }

        public B keyName(String keyName) {
            address.getProperties().put(KEY_NAME, Objects.requireNonNull(keyName));
            return self();
        }

        public DataAddress build() {
            Objects.requireNonNull(address.getType(), "DataAddress builder missing Type property.");
            return address;
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
