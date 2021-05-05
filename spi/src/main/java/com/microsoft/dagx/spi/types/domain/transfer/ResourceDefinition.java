/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.Objects;

/**
 * A resource to be provisioned to support a data transfer.
 */
public abstract class ResourceDefinition {
    protected String id;
    protected String transferProcessId;

    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<RD extends ResourceDefinition, B extends Builder<RD, B>> {
        protected final RD resourceDefinition;

        public B id(String id) {
            resourceDefinition.id = id;
            return (B) this;
        }

        public B transferProcessId(String id) {
            resourceDefinition.transferProcessId = id;
            return (B) this;
        }

        public RD build() {
            verify();
            return resourceDefinition;
        }

        protected void verify() {
            Objects.requireNonNull(resourceDefinition.id, "id");
        }

        protected Builder(RD definition) {
            this.resourceDefinition = definition;
        }
    }
}
