/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Cofinity-X - prioritized transfer services
 *
 */

package org.eclipse.edc.connector.dataplane.spi.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the registration and selection of data flow transfer services.
 */
@ExtensionPoint
public interface TransferServiceRegistry {
    
    /**
     * Adds a {@link TransferService} to the collection of services that can perform data transfers.
     * The priority is set to 0.
     *
     * @param transferService the service to add.
     */
    void registerTransferService(TransferService transferService);
    
    /**
     * Adds a {@link TransferService} with given priority to the collection of services that can
     * perform data transfers. Higher priorities will be preferred during selection.
     *
     * @param priority the priority
     * @param transferService the service to add.
     */
    void registerTransferService(int priority, TransferService transferService);

    /**
     * Resolves a {@link TransferService}s to use for serving a particular {@link DataFlowStartMessage}.
     *
     * @param request the request to resolve.
     * @return the service to be used to serve the request, or {@code null} if no service was resolved.
     */
    @Nullable
    TransferService resolveTransferService(DataFlowStartMessage request);
}
