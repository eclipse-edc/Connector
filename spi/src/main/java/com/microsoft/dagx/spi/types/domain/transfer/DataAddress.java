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
    private String type;
    private String keyName;
    private Map<String, String> properties = new HashMap<>();

    public String getType() {
        return type;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getKeyName() {
        return keyName;
    }

    private DataAddress() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private DataAddress address;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(String type) {
            address.type = type;
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
            address.keyName= Objects.requireNonNull(keyName);
            return this;
        }

        public DataAddress build() {
            Objects.requireNonNull(address.type, "type");
            Objects.requireNonNull(address.keyName, "keyName");
            return address;
        }


        private Builder() {
            address = new DataAddress();
        }
    }
}
