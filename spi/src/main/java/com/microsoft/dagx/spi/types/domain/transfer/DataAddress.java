package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

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

    public String getType() {
        return properties.get(TYPE);
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

    private DataAddress() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataAddress address;

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
        public Builder keyName(String keyName){
            address.getProperties().put(KEYNAME, Objects.requireNonNull(keyName));
            return this;
        }

        public DataAddress build() {
            Objects.requireNonNull(address.getType(), "type");
            Objects.requireNonNull(address.getKeyName(), "keyName");
            return address;
        }


        private Builder() {
            address = new DataAddress();
        }
    }
}
