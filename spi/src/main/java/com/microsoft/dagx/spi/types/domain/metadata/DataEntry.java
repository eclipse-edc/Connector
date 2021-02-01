package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DataEntry.Builder.class)
public class DataEntry<T extends DataEntryExtensions> {
    private String id;
    private T extensions;

    public String getId() {
        return id;
    }

    public T getExtensions() {
        return extensions;
    }

    private DataEntry() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<K extends DataEntryExtensions> {
        private final DataEntry<K> dataEntry;

        @JsonCreator
        public static <K extends DataEntryExtensions> Builder<K> newInstance() {
            return new Builder<K>();
        }

        public Builder<K> extensions(K extensions) {
            dataEntry.extensions = extensions;
            return this;
        }

        public Builder<K> id(String id) {
            dataEntry.id = id;
            return this;
        }

        public DataEntry<K> build() {
            return dataEntry;
        }

        private Builder() {
            dataEntry = new DataEntry<K>();

        }
    }

}
