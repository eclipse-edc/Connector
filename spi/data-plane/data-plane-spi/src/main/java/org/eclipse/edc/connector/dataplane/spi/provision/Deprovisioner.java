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

import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

/**
 * Performs deprovisioning of a specific resource type.
 */
public interface Deprovisioner {

    /**
     * Return the supported {@link ProvisionResourceDefinition} type.
     *
     * @return supported type.
     */
    String supportedType();

    /**
     * Asynchronously deprovisions a resource used to perform the data transfer.
     * Implementations must be idempotent.
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, return
     * {@link ResponseStatus#ERROR_RETRY}. If an exception occurs and re-tries should not be re-attempted, return
     * {@link ResponseStatus#FATAL_ERROR}.
     *
     * @param provisionResourceDefinition that contains metadata associated with the provision operation
     */
    CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResourceDefinition provisionResourceDefinition);

}
