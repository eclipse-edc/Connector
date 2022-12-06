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

package org.eclipse.edc.connector.provision.aws.s3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.aws.s3.S3BucketSchema;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;

import static org.eclipse.edc.aws.s3.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.edc.aws.s3.S3BucketSchema.REGION;


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

        private Builder() {
            super(new S3BucketProvisionedResource());
            dataAddressBuilder.type(S3BucketSchema.TYPE);
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
    }

}
