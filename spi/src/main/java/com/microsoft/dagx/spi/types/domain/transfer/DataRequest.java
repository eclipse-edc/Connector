package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;

/**
 * Polymorphic data request.
 */
@JsonTypeName("dagx:datarequest")
@JsonDeserialize(builder = DataRequest.Builder.class)
public class DataRequest implements Polymorphic {
    private String id;

    /**
     * The unique request id.
     */
    public String getId() {
        return id;
    }

    private DataRequest(String id) {
        this.id = id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataRequest request;

        @JsonCreator
        public static Builder newInstance(String id) {
            return new Builder(id);
        }

        public DataRequest build() {
            return request;
        }

        private Builder(String id) {
            request = new DataRequest(id);

        }
    }
}
