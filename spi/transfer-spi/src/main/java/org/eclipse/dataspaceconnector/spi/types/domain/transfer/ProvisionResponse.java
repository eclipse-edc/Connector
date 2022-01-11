package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ProvisionResponse {
    private final ProvisionedResource resource;
    private final SecretToken secretToken;

    private ProvisionResponse(ProvisionedResource resource, @Nullable SecretToken secretToken) {
        this.resource = resource;
        this.secretToken = secretToken;
    }

    public ProvisionedResource getResource() {
        return resource;
    }

    public SecretToken getSecretToken() {
        return secretToken;
    }

    public static class Builder {
        private ProvisionedResource resource;
        private SecretToken secretToken;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resource(ProvisionedResource resource) {
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
