/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 *
 */
public class ObjectStorageResourceDefinition extends ResourceDefinition {

    private String containerName;
    private String accountName;

    public String getContainerName() {
        return containerName;
    }

    public String getAccountName() {
        return accountName;
    }

    public static class Builder extends ResourceDefinition.Builder<ObjectStorageResourceDefinition, Builder> {

        private Builder() {
            super(new ObjectStorageResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder containerName(String id) {
            resourceDefinition.containerName = id;
            return this;
        }

        public Builder accountName(String accountName) {
            resourceDefinition.accountName = accountName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.containerName, "containerName");
            Objects.requireNonNull(resourceDefinition.accountName, "accountName");
        }
    }

}
