package com.microsoft.dagx.transfer.provision.azure;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

/**
 *
 */
public class ObjectContainerProvisionedResource extends ProvisionedResource {

    public static class Builder extends ProvisionedResource.Builder<ObjectContainerProvisionedResource, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ObjectContainerProvisionedResource());
        }
    }
}
