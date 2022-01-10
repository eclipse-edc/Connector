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

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

/**
 * Performs provisioning and de-provisioning of a specific resource type.
 */
public interface Provisioner<RD extends ResourceDefinition, PR extends ProvisionedResource> {

    /**
     * Initializes the provisioner with a threadsafe execution context.
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
     * Provisions a resource required to perform the data transfer, asynchronously if necessary. Results are returned via
     * {@link ProvisionContext#callback(ProvisionedResource)} or {@link ProvisionContext#callback(ProvisionedDataDestinationResource, SecretToken)}.
     * Implementations must be idempotent.
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, return
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#ERROR_RETRY}. If an exception occurs and re-tries should not be re-attempted, return
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#FATAL_ERROR}.
     */
    ResponseStatus provision(RD resourceDefinition);

    /**
     * Removes ephemeral resources of a specific type associated with the data transfer. Implements must be idempotent.
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, return
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#ERROR_RETRY}. If an exception occurs and re-tries should not be re-attempted, return
     * {@link org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus#FATAL_ERROR}.
     */
    ResponseStatus deprovision(PR provisionedResource);

}
