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

import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Performs provisioning and de-provisioning of a specific resource type.
 */
public interface Provisioner<RD extends ResourceDefinition, PR extends ProvisionedResource> {

    /**
     * Returns true if the provisioner handles the resource type.
     */
    boolean canProvision(ResourceDefinition resourceDefinition);

    /**
     * Returns true if the provisioner handles the resource type.
     */
    boolean canDeprovision(ProvisionedResource resourceDefinition);

    // TODO: fix async methods javadoc

    /**
     * Asynchronously provisions a resource required to perform the data transfer.
     * Implementations must be idempotent.
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, return
     * {@link ResponseStatus#ERROR_RETRY}. If an exception occurs and re-tries should not be re-attempted, return
     * {@link ResponseStatus#FATAL_ERROR}.
     */
    @NotNull
    CompletableFuture<ProvisionResponse> provision(RD resourceDefinition);

    /**
     * Removes ephemeral resources of a specific type associated with the data transfer. Implements must be idempotent.
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, return
     * {@link ResponseStatus#ERROR_RETRY}. If an exception occurs and re-tries should not be re-attempted, return
     * {@link ResponseStatus#FATAL_ERROR}.
     */
    @NotNull
    CompletableFuture<DeprovisionResponse> deprovision(PR provisionedResource);

}
