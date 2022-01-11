package org.eclipse.dataspaceconnector.spi.types.domain.transfer;


import java.util.Objects;

public class DeprovisionResponse {
    private final ProvisionedDataDestinationResource resource;

    private DeprovisionResponse(ProvisionedDataDestinationResource resource) {
        this.resource = resource;
    }

    public ProvisionedDataDestinationResource getResource() {
        return resource;
    }

    public static class Builder {
        private ProvisionedDataDestinationResource resource;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resource(ProvisionedDataDestinationResource resource) {
            this.resource = resource;
            return this;
        }

        public DeprovisionResponse build() {
            Objects.requireNonNull(resource, "provisionedDataDestinationResource");
            return new DeprovisionResponse(resource);
        }
    }
}
