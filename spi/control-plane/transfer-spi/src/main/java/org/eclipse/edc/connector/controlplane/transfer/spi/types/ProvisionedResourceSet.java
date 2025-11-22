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

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of provisioned resources that support a data transfer.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
@JsonTypeName("dataspaceconnector:provisionedresourceset")
@JsonDeserialize(builder = ProvisionedResourceSet.Builder.class)
public class ProvisionedResourceSet {
    private final List<ProvisionedResource> resources = new ArrayList<>();

    public List<ProvisionedResource> getResources() {
        return resources;
    }

    public void addResource(ProvisionedResource resource) {
        resources.add(resource);
    }

    public boolean empty() {
        return resources.isEmpty();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final ProvisionedResourceSet resourceSet;

        private Builder() {
            resourceSet = new ProvisionedResourceSet();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resources(List<ProvisionedResource> resources) {
            resourceSet.resources.addAll(resources);
            return this;
        }

        public ProvisionedResourceSet build() {
            return resourceSet;
        }

    }
}
