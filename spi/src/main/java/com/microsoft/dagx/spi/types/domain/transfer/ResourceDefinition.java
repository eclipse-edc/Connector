/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;

import java.util.Objects;

/**
 * A resource to be provisioned to support a data transfer.
 */
@JsonTypeName("dagx:resourcedefinition")
@JsonDeserialize(builder = ResourceDefinition.Builder.class)
public abstract class ResourceDefinition implements Polymorphic {
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

    @JsonPOJOBuilder
    @SuppressWarnings("unchecked")
    public static class Builder<RD extends ResourceDefinition, B extends Builder<RD, B>> {
        protected final RD resourceDefinition;

        protected Builder(RD definition) {
            resourceDefinition = definition;
        }

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
    }
}
