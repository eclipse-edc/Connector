/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.s3.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;


/**
 * A provisioned S3 bucket and credentials associated with a transfer process.
 */
@JsonDeserialize(builder = S3BucketProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:s3bucketprovisionedresource")
public class S3BucketProvisionedResource extends ProvisionedDataDestinationResource {
    @JsonProperty
    private String region;

    @JsonProperty
    private String bucketName;
    @JsonProperty
    private String role;

    private S3BucketProvisionedResource() {
    }

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .property(S3BucketSchema.REGION, region)
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

    public String getRole() {
        return role;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<S3BucketProvisionedResource, Builder> {

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

        public Builder role(String arn) {
            provisionedResource.role = arn;
            return this;
        }
    }

}
