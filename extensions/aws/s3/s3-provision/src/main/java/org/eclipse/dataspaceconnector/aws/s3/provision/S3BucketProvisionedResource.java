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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;


/**
 * A provisioned S3 bucket and credentials associated with a transfer process.
 */
@JsonDeserialize(builder = S3BucketProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:s3bucketprovisionedresource")
public class S3BucketProvisionedResource extends ProvisionedDataDestinationResource {
    private String role;

    public String getRegion() {
        return getDataAddress().getProperty(REGION);
    }

    public String getBucketName() {
        return getDataAddress().getProperty(BUCKET_NAME);
    }

    @Override
    public String getResourceName() {
        return dataAddress.getProperty(BUCKET_NAME);
    }

    public String getRole() {
        return role;
    }

    private S3BucketProvisionedResource() {
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<S3BucketProvisionedResource, Builder> {
        private DataAddress.Builder dataAddressBuilder;

        private Builder() {
            super(new S3BucketProvisionedResource());
            dataAddressBuilder = DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder region(String region) {
            dataAddressBuilder.property(REGION, region);
            return this;
        }

        public Builder bucketName(String bucketName) {
            dataAddressBuilder.property(BUCKET_NAME, bucketName);
            dataAddressBuilder.keyName("s3-temp-" + bucketName);
            return this;
        }

        public Builder role(String arn) {
            provisionedResource.role = arn;
            return this;
        }

        @Override
        public S3BucketProvisionedResource build() {
            provisionedResource.dataAddress = dataAddressBuilder.build();
            return super.build();
        }
    }

}
