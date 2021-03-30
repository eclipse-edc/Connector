package com.microsoft.dagx.spi.transfer.provision;

import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;

/**
 * Performs provisioning and de-provisioning of a specific resource type.
 */
public interface Provisioner<RD extends ResourceDefinition, PR extends ProvisionedResource> {

    /**
     * Initializes the provisioner with a threadsafe excution context.
     *
     * This context is used to persist recovery data and return results when {@link #provision(ResourceDefinition)} completes.
     *
     * @param context the provision context
     */
    void initialize(ProvisionContext context);

    /**
     * Returns true if the provisioner handles the resource type.
     */
    boolean canProvision(ResourceDefinition resourceDefinition);

    /**
     * Returns true if the provisioner handles the resource type.
     */
    boolean canDeprovision(ProvisionedResource resourceDefinition);

    /**
     * Provisions a resource required to perform the data transfer, asynchronously if necessary. Results are returned via {@link ProvisionContext#callback(ProvisionedResource)}.
     * Implements must be idempotent.
     */
    ResponseStatus provision(RD resourceDefinition);

    /**
     * Removes ephemeral resources of a specific type associated with the data transfer. Implements must be idempotent.
     */
    ResponseStatus deprovision(PR provisionedResource);

}
