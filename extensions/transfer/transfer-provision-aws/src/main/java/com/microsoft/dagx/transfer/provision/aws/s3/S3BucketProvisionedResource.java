package com.microsoft.dagx.transfer.provision.aws.s3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.transfer.DataDestination;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;

import static com.microsoft.dagx.transfer.provision.aws.s3.S3Destination.BUCKET_NAME;
import static com.microsoft.dagx.transfer.provision.aws.s3.S3Destination.REGION;
import static com.microsoft.dagx.transfer.provision.aws.s3.S3Destination.TYPE;

/**
 * A provisioned S3 bucket and credentials associated with a transfer process.
 */
@JsonDeserialize(builder = S3BucketProvisionedResource.Builder.class)
@JsonTypeName("dagx:s3bucketprovisionedresource")
public class S3BucketProvisionedResource extends ProvisionedDataDestinationResource {
    @JsonProperty
    private String region;

    @JsonProperty
    private String bucketName;

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public DataDestination createDataDestination() {
        return DataDestination.Builder.newInstance().property(REGION, region).type(TYPE).property(BUCKET_NAME, bucketName).build();
    }

    private S3BucketProvisionedResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<S3BucketProvisionedResource, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder region(String region) {
            provisionedResource.region = region;
            return this;
        }

        public Builder bucketName(String bucketName) {
            provisionedResource.bucketName = bucketName;
            return this;
        }

        private Builder() {
            super(new S3BucketProvisionedResource());
        }
    }
}
