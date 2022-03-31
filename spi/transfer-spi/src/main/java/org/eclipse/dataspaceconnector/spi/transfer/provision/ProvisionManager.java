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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    CompletableFuture<List<ProvisionResult>> provision(List<ResourceDefinition> definitions, Policy policy);

    /**
     * Removes ephemeral resources associated with the data transfer. This operation is idempotent.
     */
    CompletableFuture<List<DeprovisionResult>> deprovision(List<ProvisionedResource> resources, Policy policy);
}
