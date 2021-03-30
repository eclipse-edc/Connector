package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.Set;

/**
 * A collection of provisioned resources that support a data transfer.
 */
public class ProvisionedResourceSet {
    private String id;
    private String transferProcessId;

    private Set<ProvisionedResource> resources;

    public String getId() {
        return id;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public Set<ProvisionedResource> getResources() {
        return resources;
    }
}
