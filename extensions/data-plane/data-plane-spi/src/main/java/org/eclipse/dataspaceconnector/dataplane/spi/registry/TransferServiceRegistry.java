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
 *
 */
package org.eclipse.dataspaceconnector.dataplane.spi.registry;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the registration and selection of data flow transfer services.
 */
public interface TransferServiceRegistry {
    /**
     * Adds a {@link TransferService} to the collection of services that can perform data transfers.
     *
     * @param transferService the service to add.
     */
    void registerTransferService(TransferService transferService);


    /**
     * Resolves a {@link TransferService}s to use for serving a particular {@link DataFlowRequest}.
     *
     * @param request the request to resolve.
     * @return the service to be used to serve the request, or {@code null} if no service was resolved.
     */
    @Nullable
    TransferService resolveTransferService(DataFlowRequest request);
}
