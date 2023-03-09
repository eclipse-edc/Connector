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

package org.eclipse.edc.connector.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of provisioned resources that support a data transfer.
 *
 * @deprecated a simple list of {@link ProvisionedResource} can be used instead.
 */
@JsonTypeName("dataspaceconnector:provisionedresourceset")
@JsonDeserialize(builder = ProvisionedResourceSet.Builder.class)
@Deprecated(since = "milestone9")
public class ProvisionedResourceSet {
    private String transferProcessId;

    private final List<ProvisionedResource> resources = new ArrayList<>();

    public String getTransferProcessId() {
        return transferProcessId;
    }

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
        resources.forEach(r -> r.setTransferProcessId(transferProcessId));
    }

    public List<ProvisionedResource> getResources() {
        return resources;
    }

    public void addResource(ProvisionedResource resource) {
        if (transferProcessId != null) {
            resource.setTransferProcessId(transferProcessId);
        }
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

        public Builder transferProcessId(String id) {
            resourceSet.setTransferProcessId(id);
            return this;
        }

        public ProvisionedResourceSet build() {
            return resourceSet;
        }

    }
}
