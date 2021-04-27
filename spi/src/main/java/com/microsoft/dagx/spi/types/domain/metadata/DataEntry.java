package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Data that is managed and can be shared by the system.
 *
 * @param <T> domain-specific extension properties.
 */
@JsonDeserialize(builder = DataEntry.Builder.class)
public class DataEntry<T extends DataEntryPropertyLookup> {
    private String id;
    private T lookup;

    public String getId() {
        return id;
    }

    public T getLookup() {
        return lookup;
    }

    private DataEntry() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<K extends DataEntryPropertyLookup> {
        private final DataEntry<K> dataEntry;

        @JsonCreator
        public static <K extends DataEntryPropertyLookup> Builder<K> newInstance() {
            return new Builder<K>();
        }

        public Builder<K> lookup(K extensions) {
            dataEntry.lookup = extensions;
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
