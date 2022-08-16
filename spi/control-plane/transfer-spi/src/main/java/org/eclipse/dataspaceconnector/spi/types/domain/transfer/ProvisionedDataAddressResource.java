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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import static java.util.Objects.requireNonNull;

/**
 * A provisioned resource that is referenced using a {@link DataAddress}.
 */
public abstract class ProvisionedDataAddressResource extends ProvisionedResource {
    protected String resourceName;
    protected DataAddress dataAddress;

    public String getResourceName() {
        return resourceName;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    protected ProvisionedDataAddressResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<T extends ProvisionedDataAddressResource, B extends Builder<T, B>> extends ProvisionedResource.Builder<T, B> {

        @SuppressWarnings("unchecked")
        public B resourceName(String name) {
            provisionedResource.resourceName = name;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B dataAddress(DataAddress dataAddress) {
            provisionedResource.dataAddress = dataAddress;
            return (B) this;
        }

        protected Builder(T resource) {
            super(resource);
        }

        @Override
        protected void verify() {
            requireNonNull(provisionedResource.resourceName, "resourceName");
            requireNonNull(provisionedResource.dataAddress, "dataAddress");
        }

    }
}
