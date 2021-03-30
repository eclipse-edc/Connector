package com.microsoft.dagx.spi.types.domain.transfer;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A provisioned resource that supports a data transfer request.
 */
public abstract class ProvisionedResource {
    private String id;
    private String transferProcessId;
    private String resourceDefinitionId;
    private boolean error;
    private String errorMessage;

    @NotNull
    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    @NotNull
    public String getResourceDefinitionId() {
        return resourceDefinitionId;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<PR extends ProvisionedResource, B extends Builder<PR, B>> {
        protected final ProvisionedResource provisionedResource;

        public B id(String id) {
            provisionedResource.id = id;
            return (B) this;
        }

        public B transferProcessId(String id) {
            provisionedResource.transferProcessId = id;
            return (B) this;
        }

        public B resourceDefinitionId(String id) {
            provisionedResource.resourceDefinitionId = id;
            return (B) this;
        }

        public B error(boolean value) {
            provisionedResource.error = value;
            return (B) this;
        }

        public B errorMessage(String errorMessage) {
            provisionedResource.errorMessage = errorMessage;
            return (B) this;
        }

        public PR build() {
            Objects.requireNonNull(provisionedResource.id, "id");
            Objects.requireNonNull(provisionedResource.resourceDefinitionId, "resourceDefinitionId");
            return (PR) provisionedResource;
        }

        protected Builder(ProvisionedResource resource) {
            this.provisionedResource = resource;
        }
    }

}
