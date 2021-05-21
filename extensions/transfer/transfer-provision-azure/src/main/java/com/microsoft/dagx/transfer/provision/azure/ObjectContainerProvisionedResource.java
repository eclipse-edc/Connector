/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

/**
 *
 */
public class ObjectContainerProvisionedResource extends ProvisionedDataDestinationResource {

    @JsonProperty
    private String accountName;
    @JsonProperty
    private String containerName;

    public String getAccountName() {
        return accountName;
    }

    public String getContainerName() {
        return containerName;
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .keyName(getResourceName())
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, accountName)
                .build();
    }

    @Override
    public String getResourceName() {
        return containerName;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedResource.Builder<ObjectContainerProvisionedResource, Builder> {

        private Builder() {
            super(new ObjectContainerProvisionedResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            provisionedResource.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            provisionedResource.containerName = containerName;
            return this;
        }

    }
}
