package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;

/**
 * Polymorphic data request.
 */
@JsonTypeName("dagx:datarequest")
@JsonDeserialize(builder = DataRequest.Builder.class)
public class DataRequest implements Polymorphic {
    private String id;
    private DataEntry<?> dataEntry;

    /**
     * The unique request id.
     */
    public String getId() {
        return id;
    }

    public DataEntry<?> getDataEntry() {
        return dataEntry;
    }

    private DataRequest() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataRequest request;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public DataRequest build() {
            return request;
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder dataEntry(DataEntry<?> entry) {
            request.dataEntry = entry;
            return this;
        }

        private Builder() {
            request = new DataRequest();
        }
    }
}
