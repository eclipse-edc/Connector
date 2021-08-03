/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.provision.azure.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.transfer.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.edc.schema.azure.*;

@JsonDeserialize(builder = ObjectContainerProvisionedResource.Builder.class)
@JsonTypeName("edc:objectcontainerprovisionedresource")
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
