/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.provision;

import org.eclipse.edc.spi.response.StatusResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages resource provisioning for a data flow.
 */
public interface ProvisionerManager {

    /**
     * Registers the provisioner.
     */
    void register(Provisioner provisioner);

    /**
     * Registers the provisioner.
     */
    void register(Deprovisioner deprovisioner);

    /**
     * Provisions resources required to perform the data flow. This operation is idempotent.
     */
    CompletableFuture<List<StatusResult<ProvisionedResource>>> provision(List<ProvisionResourceDefinition> definitions);

    /**
     * Deprovisions resources required to perform the data flow. This operation is idempotent.
     */
    CompletableFuture<List<StatusResult<DeprovisionedResource>>> deprovision(List<ProvisionResourceDefinition> definitions);

}
