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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


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
    public static final String SIMPLE_KEY_NAME = "keyName";
    public static final String SIMPLE_PROPERTIES_KEY = "properties";
    public static final String EDC_DATA_ADDRESS_TYPE = EDC_NAMESPACE + "DataAddress";
    public static final String EDC_DATA_ADDRESS_TYPE_PROPERTY = EDC_NAMESPACE + SIMPLE_TYPE;
    public static final String EDC_DATA_ADDRESS_KEY_NAME = EDC_NAMESPACE + SIMPLE_KEY_NAME;
    public static final String EDC_DATA_ADDRESS_SECRET = EDC_NAMESPACE + "secret";
    public static final String EDC_DATA_ADDRESS_RESPONSE_CHANNEL = EDC_NAMESPACE + "responseChannel";

    protected final Map<String, Object> properties = new HashMap<>();

    protected DataAddress() {
    }

    @NotNull
    public String getType() {
        return getStringProperty(EDC_DATA_ADDRESS_TYPE_PROPERTY);
    }

    @JsonIgnore
    public void setType(String type) {
        Objects.requireNonNull(type);
        properties.put(EDC_DATA_ADDRESS_TYPE_PROPERTY, type);
    }

    @Nullable
    public String getStringProperty(String key) {
        return getStringProperty(key, null);
    }

    @Nullable
    public String getStringProperty(String key, String defaultValue) {
        var value = getProperty(key);
        if (value != null) {
            return (String) value;
        }
        return defaultValue;
    }

    @Nullable
    public Object getProperty(String key) {
        return Optional.ofNullable(properties.get(EDC_NAMESPACE + key)).orElseGet(() -> properties.get(key));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getKeyName() {
        return getStringProperty(EDC_DATA_ADDRESS_KEY_NAME);
    }

    @JsonIgnore
    public DataAddress getResponseChannel() {
        return (DataAddress) getProperty(EDC_DATA_ADDRESS_RESPONSE_CHANNEL);
    }

    @JsonIgnore
    public void setKeyName(String keyName) {
        Objects.requireNonNull(keyName);
        properties.put(EDC_DATA_ADDRESS_KEY_NAME, keyName);
    }

    /**
     * Returns true if there's a property with the specified key
     *
     * @param key the key
     * @return true if it exists, false if not
     */
    @JsonIgnore
    public boolean hasProperty(String key) {
        return getStringProperty(key) != null;
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
            address.properties.put(EDC_DATA_ADDRESS_TYPE_PROPERTY, Objects.requireNonNull(type));
            return self();
        }

        public B property(String key, Object value) {
            Objects.requireNonNull(key, "Property key null.");
            switch (key) {
                case SIMPLE_TYPE -> address.properties.put(EDC_DATA_ADDRESS_TYPE_PROPERTY, value);
                case SIMPLE_KEY_NAME -> address.properties.put(EDC_DATA_ADDRESS_KEY_NAME, value);
                case EDC_DATA_ADDRESS_RESPONSE_CHANNEL -> checkAndCreateResponseChannel(value);
                default -> address.properties.put(key, value);
            }
            return self();
        }

        public B properties(Map<String, Object> properties) {
            properties.forEach(this::property);
            return self();
        }

        public B keyName(String keyName) {
            address.getProperties().put(EDC_DATA_ADDRESS_KEY_NAME, Objects.requireNonNull(keyName));
            return self();
        }

        @JsonIgnore
        public B responseChannel(DataAddress rc) {
            address.properties.put(EDC_DATA_ADDRESS_RESPONSE_CHANNEL, rc);
            return self();

        }

        @JsonIgnore
        @SuppressWarnings("unchecked")
        public B responseChannel(Map<String, Object> rc) {
            var builder = DataAddress.Builder.newInstance();
            var props = rc.get(SIMPLE_PROPERTIES_KEY);
            if (props != null) {
                builder.properties((Map<String, Object>) props);
            } else {
                builder.properties(rc);
            }
            address.properties.put(EDC_DATA_ADDRESS_RESPONSE_CHANNEL, builder.build());

            return self();
        }

        public DataAddress build() {
            Objects.requireNonNull(address.getType(), "DataAddress builder missing Type property.");
            return address;
        }

        @SuppressWarnings("unchecked")
        private void checkAndCreateResponseChannel(Object object) {
            if (object instanceof DataAddress da) {
                responseChannel(da);
            } else if (object instanceof Map map) {
                responseChannel(map);
            } else {
                throw new IllegalArgumentException("Response channel must be a DataAddress or a Map.");
            }
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
