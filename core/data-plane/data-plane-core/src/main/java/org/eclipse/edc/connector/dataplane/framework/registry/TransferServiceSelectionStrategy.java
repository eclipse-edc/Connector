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
 *       Microsoft Corporation - Initial implementation
 *       Cofinity-X - prioritized transfer services
 *
 */

package org.eclipse.edc.connector.dataplane.framework.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Functional interface for selecting which of (potentially) multiple {@link TransferService}s to use
 * for serving a particular {@link DataFlowStartMessage}.
 *
 * @deprecated use transfer service prioritization, see
 * {@link org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry#registerTransferService(int, TransferService)}.
 */
@Deprecated(since = "0.12.0", forRemoval = true)
public interface TransferServiceSelectionStrategy {
    /**
     * Selects which of (potentially) multiple {@link TransferService}s to use
     * for serving a particular {@link DataFlowStartMessage}.
     *
     * @param request          the request.
     * @param transferServices any number of services which are able to handle the request. May be an empty {@link Stream}.
     * @return the service to be used to serve the request, selected among the input {@code transferServices}, or {@code null} if the stream is empty or no service should be used.
     */
    @Nullable
    TransferService chooseTransferService(DataFlowStartMessage request, Stream<TransferService> transferServices);

    /**
     * Default strategy: use first matching service. This allows integrators to select
     * order preferred {@link TransferService}s in the classpath.
     *
     * @return the first service, or {@code null} if the stream is empty.
     */
    static TransferServiceSelectionStrategy selectFirst() {
        return (request, transferServices) -> transferServices.findFirst().orElse(null);
    }
}
