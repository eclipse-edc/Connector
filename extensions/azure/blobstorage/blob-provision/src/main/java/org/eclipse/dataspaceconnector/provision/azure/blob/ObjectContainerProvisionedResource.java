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

package org.eclipse.dataspaceconnector.provision.azure.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;

import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema.CONTAINER_NAME;

@JsonDeserialize(builder = ObjectContainerProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:objectcontainerprovisionedresource")
public class ObjectContainerProvisionedResource extends ProvisionedDataDestinationResource {

    public String getAccountName() {
        return getDataAddress().getProperty(ACCOUNT_NAME);
    }

    public String getContainerName() {
        return getDataAddress().getProperty(CONTAINER_NAME);
    }

    private ObjectContainerProvisionedResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<ObjectContainerProvisionedResource, Builder> {
        private DataAddress.Builder dataAddressBuilder;

        private Builder() {
            super(new ObjectContainerProvisionedResource());
            dataAddressBuilder = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            this.dataAddressBuilder.property(ACCOUNT_NAME, accountName);
            return this;
        }

        public Builder containerName(String containerName) {
            this.dataAddressBuilder.property(CONTAINER_NAME, containerName);
            return this;
        }

        @Override
        public Builder resourceName(String name) {
            this.dataAddressBuilder.keyName(name);
            super.resourceName(name);
            return this;
        }

        @Override
        public ObjectContainerProvisionedResource build() {
            provisionedResource.dataAddress = dataAddressBuilder.build();
            return super.build();
        }
    }
}
