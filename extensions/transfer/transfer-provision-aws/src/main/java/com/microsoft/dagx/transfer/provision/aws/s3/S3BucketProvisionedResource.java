package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

/**
 * A provisioned S3 bucket and credentials associated with a transfer process.
 */
public class S3BucketProvisionedResource extends ProvisionedResource {

    private S3BucketProvisionedResource() {
    }

    public static class Builder extends ProvisionedResource.Builder<S3BucketProvisionedResource, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new S3BucketProvisionedResource());
        }
    }
}
