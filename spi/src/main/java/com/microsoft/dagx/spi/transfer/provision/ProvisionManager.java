package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResourceSet;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;

/**
 * Manages resource provisioning for a data transfer.
 */
public interface ProvisionManager {

    /**
     * Registers the provisioner.
     */
    <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner);

    /**
     * Provisions resources required to perform the data transfer. This operation is idempotent.
     */
    void provision(ResourceManifest manifest);

    /**
     * Removes ephemeral resources associated with the data transfer. this operation is idempotent.
     */
    void deprovision(ProvisionedResourceSet set);
}
