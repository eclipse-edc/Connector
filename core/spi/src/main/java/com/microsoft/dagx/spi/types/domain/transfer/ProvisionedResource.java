/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A provisioned resource that supports a data transfer request.
 */
@JsonTypeName("dagx:provisionedresource")
@JsonDeserialize(builder = ProvisionedResource.Builder.class)
public abstract class ProvisionedResource implements Polymorphic {
    protected String id;
    protected String transferProcessId;
    protected String resourceDefinitionId;
    protected boolean error;
    protected String errorMessage;

    @NotNull
    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
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


    @JsonPOJOBuilder
    @SuppressWarnings("unchecked")
    public static class Builder<PR extends ProvisionedResource, B extends Builder<PR, B>> {
        protected PR provisionedResource;

        protected Builder(PR resource) {
            provisionedResource = resource;
        }

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
            Objects.requireNonNull(provisionedResource.transferProcessId, "transferProcessId");
            Objects.requireNonNull(provisionedResource.resourceDefinitionId, "resourceDefinitionId");
            verify();
            return provisionedResource;
        }

        protected void verify() {
        }
    }

}
