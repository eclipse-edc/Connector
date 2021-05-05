/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

/**
 *
 */
public class ObjectContainerProvisionedResource extends ProvisionedDataDestinationResource {

    @Override
    public DataAddress createDataDestination() {
        return null;
    }

    public static class Builder extends ProvisionedResource.Builder<ObjectContainerProvisionedResource, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ObjectContainerProvisionedResource());
        }
    }
}
