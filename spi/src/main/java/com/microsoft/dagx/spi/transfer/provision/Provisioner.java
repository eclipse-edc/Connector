package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;

/**
 * Performs provisioning and de-provisioning of a specific resource type.
 */
public interface Provisioner {

    /**
     * Provisions a resource required to perform the data transfer. Implements must be idempotent.
     */
    ProvisionedResource provision(ResourceDefinition resourceDefinition);

    /**
     * Removes ephemeral resources of a specific type associated with the data transfer. Implements must be idempotent.
     */
    void deprovision(ProvisionedResource provisionedResource);

}
