package com.microsoft.dagx.transfer.types.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataDestination;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;

/**
 *
 */
@JsonDeserialize(builder = S3Destination.Builder.class)
@JsonTypeName("dagx:s3destination")
public class S3Destination implements DataDestination {
    private String region;
    private String bucketName;
    private DestinationSecretToken secretToken;

    @Override
    @JsonProperty
    public String getType() {
        return "S3";
    }

    @Override
    public DestinationSecretToken getSecretToken() {
        return secretToken;
    }

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }

    private S3Destination() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private S3Destination destination;

        @JsonCreator
        public static <K extends DataEntryExtensions> S3Destination.Builder newInstance() {
            return new Builder();
        }

        public Builder region(String region) {
            this.destination.region = region;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.destination.bucketName = bucketName;
            return this;
        }

        public Builder secretToken(DestinationSecretToken token) {
            this.destination.secretToken = token;
            return this;
        }

        public S3Destination build() {
            return destination;
        }

        private Builder() {
            destination = new S3Destination();
        }
    }
}
