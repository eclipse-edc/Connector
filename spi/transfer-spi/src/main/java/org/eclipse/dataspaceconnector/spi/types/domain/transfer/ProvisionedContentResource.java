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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.Objects;

/**
 * A provisioned resource that is a content data address.
 *
 * This resource type is created when a provider's backend system provisions data as part of a data transfer.
 */
@JsonTypeName("dataspaceconnector:provisioneddcontentresource")
@JsonDeserialize(builder = ProvisionedContentResource.Builder.class)
public class ProvisionedContentResource extends ProvisionedResource {
    private String resourceName;
    private DataAddress contentDataAddress;

    public String getResourceName() {
        return resourceName;
    }

    public DataAddress getContentDataAddress() {
        return contentDataAddress;
    }

    private ProvisionedContentResource() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedResource.Builder<ProvisionedContentResource, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resourceName(String name) {
            provisionedResource.resourceName = name;
            return this;
        }

        public Builder contentDataAddress(DataAddress dataAddress) {
            provisionedResource.contentDataAddress = dataAddress;
            return this;
        }

        @Override
        protected void verify() {
            Objects.requireNonNull(provisionedResource.resourceName, "resourceName");
        }

        private Builder() {
            super(new ProvisionedContentResource());
        }

    }
}
