/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.Polymorphic;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A provisioned resource that supports a data transfer request.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:provisionedresource")
@JsonDeserialize(builder = ProvisionedResource.Builder.class)
public abstract class ProvisionedResource implements Polymorphic {
    protected String id;
    protected String transferProcessId;
    protected String resourceDefinitionId;
    protected boolean hasToken;

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

    public boolean hasToken() {
        return hasToken;
    }

    @JsonPOJOBuilder
    public static class Builder<PR extends ProvisionedResource, B extends Builder<PR, B>> {
        protected PR provisionedResource;

        protected Builder(PR resource) {
            provisionedResource = resource;
        }

        @SuppressWarnings("unchecked")
        public B id(String id) {
            provisionedResource.id = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B transferProcessId(String id) {
            provisionedResource.transferProcessId = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resourceDefinitionId(String id) {
            provisionedResource.resourceDefinitionId = id;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B hasToken(boolean value) {
            provisionedResource.hasToken = value;
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
