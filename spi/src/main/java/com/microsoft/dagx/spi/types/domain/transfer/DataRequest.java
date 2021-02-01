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

        private Builder() {
            request = new DataRequest();
        }
    }
}
