/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.provision.http.impl;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.Objects;

/**
 * Defines common attributes for HTTP-based provisioning asset resources.
 */
public abstract class AbstractHttpResourceDefinition extends ResourceDefinition {
    protected String dataAddressType;

    protected AbstractHttpResourceDefinition() {
    }

    /**
     * Returns the data address type the definition is associated with. This is used to determine which provisioner will be engaged.
     */
    public String getDataAddressType() {
        return dataAddressType;
    }

    @Override
    public String getTransferProcessId() {
        return transferProcessId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<RD extends AbstractHttpResourceDefinition, B extends ResourceDefinition.Builder<RD, B>> extends ResourceDefinition.Builder<RD, B> {

        protected Builder(RD definition) {
            super(definition);
        }

        @SuppressWarnings("unchecked")
        public B dataAddressType(String dataAddressType) {
            resourceDefinition.dataAddressType = dataAddressType;
            return (B) this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.dataAddressType, "transferType");
        }

    }

}
