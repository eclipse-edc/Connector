/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of provisioned resources that support a data transfer.
 */
@JsonTypeName("edc:provisionedresourceset")
@JsonDeserialize(builder = ProvisionedResourceSet.Builder.class)
public class ProvisionedResourceSet {
    private String transferProcessId;

    private List<ProvisionedResource> resources = new ArrayList<>();

    public String getTransferProcessId() {
        return transferProcessId;
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

    void setTransferProcessId(String transferProcessId) {
        this.transferProcessId = transferProcessId;
        resources.forEach(r -> r.setTransferProcessId(transferProcessId));
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ProvisionedResourceSet resourceSet;

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

        private Builder() {
            resourceSet = new ProvisionedResourceSet();
        }

    }
}
