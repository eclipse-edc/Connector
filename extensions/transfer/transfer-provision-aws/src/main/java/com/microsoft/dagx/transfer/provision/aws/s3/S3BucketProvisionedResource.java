/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.aws.s3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.spi.types.domain.transfer.CompletionChecker;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;


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

    private S3CompletionChecker checker;

    private S3BucketProvisionedResource() {
    }

    private S3BucketProvisionedResource(ClientProvider clientProvider, RetryPolicy<Object> retryPolicy) {
        checker = new S3CompletionChecker(clientProvider, retryPolicy, region, bucketName);
    }

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance().property(S3BucketSchema.REGION, region)
                .type(S3BucketSchema.TYPE)
                .property(S3BucketSchema.BUCKET_NAME, bucketName)
                .keyName("s3-temp-" + bucketName)
                .build();
    }

    @JsonIgnore
    @Override
    public String getResourceName() {
        return bucketName;
    }

    @Override
    public CompletionChecker getCompletionChecker() {
        return checker;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<S3BucketProvisionedResource, Builder> {

        private ClientProvider clientProvider;
        private RetryPolicy<Object> retryPolicy;

        private Builder() {
            super(new S3BucketProvisionedResource());
        }

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

        public Builder checker(ClientProvider clientProvider, RetryPolicy<Object> retryPolicy) {
            this.clientProvider = clientProvider;
            this.retryPolicy = retryPolicy;
            return this;
        }

        @Override
        public S3BucketProvisionedResource build() {
            var res = super.build();
            res.checker = new S3CompletionChecker(clientProvider, retryPolicy, res.getRegion(), res.getBucketName());
            return res;
        }
    }

    private static class S3CompletionChecker implements CompletionChecker {
        private final S3AsyncClient s3client;
        private final RetryPolicy<Object> retryPolicy;
        private final String bucketName;

        public S3CompletionChecker(ClientProvider clientProvider, RetryPolicy<Object> retryPolicy, String region, String bucketName) {
            this.retryPolicy = retryPolicy;
            this.bucketName = bucketName;
            s3client = clientProvider.clientFor(S3AsyncClient.class, region);
        }

        @Override
        public boolean check() {
            return checkForCompleteFile();
        }

        private boolean checkForCompleteFile() {
            try {
                var rq = ListObjectsRequest.builder().bucket(bucketName).build();
                var response = Failsafe.with(retryPolicy).get(() -> s3client.listObjects(rq).join());
                return response.contents().stream().anyMatch(s3object -> s3object.key().endsWith(".complete"));
            } catch (NoSuchBucketException ex) {
                return false;
            }
        }
    }
}
