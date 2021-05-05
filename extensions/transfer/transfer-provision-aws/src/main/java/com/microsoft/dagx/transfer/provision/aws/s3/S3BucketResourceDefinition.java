/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 * An S3 bucket and access credentials to be provisioned.
 */
public class S3BucketResourceDefinition extends ResourceDefinition {
    private String regionId;
    private String bucketName;

    public String getRegionId() {
        return regionId;
    }

    public String getBucketName() {
        return bucketName;
    }

    private S3BucketResourceDefinition() {
    }

    public static class Builder extends ResourceDefinition.Builder<S3BucketResourceDefinition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder regionId(String regionId) {
            resourceDefinition.regionId = regionId;
            return this;
        }

        public Builder bucketName(String bucketName) {
            resourceDefinition.bucketName = bucketName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.regionId, "regionId");
            Objects.requireNonNull(resourceDefinition.bucketName, "bucketName");
        }

        private Builder() {
            super(new S3BucketResourceDefinition());
        }
    }

}
