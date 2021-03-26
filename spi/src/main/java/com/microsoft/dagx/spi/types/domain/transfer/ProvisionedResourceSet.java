package com.microsoft.dagx.spi.types.domain.transfer;

import java.util.Set;

/**
 * A collection of provisioned resources that support a data transfer.
 */
public class ProvisionedResourceSet {

    private Set<ProvisionedResource> resources;

    public Set<ProvisionedResource> getResources() {
        return resources;
    }
}
