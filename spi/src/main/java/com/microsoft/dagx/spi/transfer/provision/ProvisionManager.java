package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResourceSet;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

/**
 * Manages resource provisioning for a {@link TransferProcess}
 */
public interface ProvisionManager {

    /**
     * Registers the provisioner.
     */
    void register(Provisioner provisioner);

    /**
     * Provisions resources required to perform the data transfer. This operation is idempotent.
     */
    void provision(ResourceManifest manifest);

    /**
     * Removes ephemeral resources associated with the data transfer. this operation is idempotent.
     */
    void deprovision(ProvisionedResourceSet set);
}
