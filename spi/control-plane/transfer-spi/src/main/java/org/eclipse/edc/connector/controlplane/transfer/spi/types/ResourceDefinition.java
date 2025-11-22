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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - add toBuilder method
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.Polymorphic;

import java.util.Objects;

/**
 * A resource to be provisioned to support a data transfer.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:resourcedefinition")
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

    /**
     * Converts the resource definition to a builder to allow for easy modification.
     *
     * @param <RD> the type of resource definition.
     * @param <B> the respective builder type.
     * @return the builder.
     */
    public abstract <RD extends ResourceDefinition, B extends Builder<RD, B>> B toBuilder();

    /**
     * Sets the base class properties on a sub class builder.
     *
     * @param builder the builder.
     * @param <RD> the type of resource definition.
     * @param <B> the respective builder type.
     * @return the builder with class properties set.
     */
    protected <RD extends ResourceDefinition, B extends Builder<RD, B>> B initializeBuilder(B builder) {
        return builder
                .id(id)
                .transferProcessId(transferProcessId);
    }

    @JsonPOJOBuilder
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
