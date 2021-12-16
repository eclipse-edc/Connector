package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.Objects;

public class DeprovisionResponse {
    private final ResponseStatus status;
    private final ProvisionedDataDestinationResource resource;

    private DeprovisionResponse(ResponseStatus status, ProvisionedDataDestinationResource resource) {
        this.status = status;
        this.resource = resource;
    }

    public ProvisionedDataDestinationResource getResource() {
        return resource;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public static class Builder {
        private ProvisionedDataDestinationResource resource;
        private ResponseStatus status;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder ok() {
            this.status = ResponseStatus.OK;
            return this;
        }

        public Builder resource(ProvisionedDataDestinationResource resource) {
            this.resource = resource;
            return this;
        }

        public DeprovisionResponse build() {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(resource, "provisionedDataDestinationResource");
            return new DeprovisionResponse(status, resource);
        }
    }
}
