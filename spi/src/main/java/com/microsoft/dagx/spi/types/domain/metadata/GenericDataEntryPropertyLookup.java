package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic extension properties.
 */
@JsonTypeName("dagx:genericdataentryextensions")
@JsonDeserialize(builder = GenericDataEntryPropertyLookup.Builder.class)
public class GenericDataEntryPropertyLookup extends DataEntryPropertyLookup {
    private Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Map<String, Object> getPropertiesForEntity(String id) {
        return null;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private GenericDataEntryPropertyLookup extensions;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder property(String key, String value) {
            extensions.properties.put(key, value);
            return this;
        }

        public GenericDataEntryPropertyLookup build() {
            return extensions;
        }

        private Builder() {
            extensions = new GenericDataEntryPropertyLookup();
        }
    }


}
