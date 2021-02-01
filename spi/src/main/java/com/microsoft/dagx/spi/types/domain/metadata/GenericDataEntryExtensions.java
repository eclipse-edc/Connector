package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("dagx:genericdataentryextensions")
@JsonDeserialize(builder = GenericDataEntryExtensions.Builder.class)
public class GenericDataEntryExtensions extends DataEntryExtensions {
    private Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private GenericDataEntryExtensions extensions;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder property(String key, String value) {
            extensions.properties.put(key, value);
            return this;
        }

        public GenericDataEntryExtensions build() {
            return extensions;
        }

        private Builder() {
            extensions = new GenericDataEntryExtensions();
        }
    }


}
