package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;

/**
 * Performs provisioning and de-provisioning of a specific resource type.
 */
public interface Provisioner {

    /**
     * Provisions resources of a specific type required to perform the data transfer. Implements must be idempotent.
     */
    void provision(TransferProcess process);

    /**
     * Removes ephemeral resources of a specific type associated with the data transfer. Implements must be idempotent.
     */
    void deProvision(TransferProcess process);

}
