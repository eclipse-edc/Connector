package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ProvisionResponse {
    private final ProvisionedDataDestinationResource resource;
    private final SecretToken secretToken;

    private ProvisionResponse(ProvisionedDataDestinationResource resource, @Nullable SecretToken secretToken) {
        this.resource = resource;
        this.secretToken = secretToken;
    }

    public ProvisionedDataDestinationResource getResource() {
        return resource;
    }

    public SecretToken getSecretToken() {
        return secretToken;
    }

    public static class Builder {
        private ProvisionedDataDestinationResource resource;
        private SecretToken secretToken;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resource(ProvisionedDataDestinationResource resource) {
            this.resource = resource;
            return this;
        }

        public Builder secretToken(SecretToken secretToken) {
            this.secretToken = secretToken;
            return this;
        }

        public ProvisionResponse build() {
            Objects.requireNonNull(resource, "provisionedDataDestinationResource");
            return new ProvisionResponse(resource, secretToken);
        }
    }
}
