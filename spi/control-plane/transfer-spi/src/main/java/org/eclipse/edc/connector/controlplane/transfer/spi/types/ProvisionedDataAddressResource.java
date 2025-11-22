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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

import static java.util.Objects.requireNonNull;

/**
 * A provisioned resource that is referenced using a {@link DataAddress}.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
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
        protected DataAddress.Builder dataAddressBuilder = DataAddress.Builder.newInstance();

        @SuppressWarnings("unchecked")
        public B resourceName(String name) {
            provisionedResource.resourceName = name;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B dataAddress(DataAddress dataAddress) {
            dataAddress.getProperties().forEach(dataAddressBuilder::property);
            return (B) this;
        }

        protected Builder(T resource) {
            super(resource);
        }

        @Override
        public T build() {
            provisionedResource.dataAddress = dataAddressBuilder.build();
            return super.build();
        }

        @Override
        protected void verify() {
            requireNonNull(provisionedResource.resourceName, "resourceName");
            requireNonNull(provisionedResource.dataAddress, "dataAddress");
        }

    }
}
