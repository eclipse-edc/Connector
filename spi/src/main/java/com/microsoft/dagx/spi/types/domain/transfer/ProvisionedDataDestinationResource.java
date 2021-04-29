package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A provisioned resource that serves as a data destination.
 */
public abstract class ProvisionedDataDestinationResource extends ProvisionedResource {

    public abstract DataDestination createDataDestination();

    protected ProvisionedDataDestinationResource() {
        super();
    }

    protected static class Builder<PR extends ProvisionedResource, B extends ProvisionedResource.Builder<PR, B>> extends  ProvisionedResource.Builder<PR, B> {

        protected Builder(PR resource) {
            super(resource);
        }

    }
}
