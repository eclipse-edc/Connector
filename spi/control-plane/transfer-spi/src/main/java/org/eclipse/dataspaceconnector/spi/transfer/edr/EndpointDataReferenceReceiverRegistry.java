/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Registry for {@link EndpointDataReferenceReceiver}.
 */
public interface EndpointDataReferenceReceiverRegistry {
    /**
     * Adds a new {@link EndpointDataReferenceReceiver} into the registry.
     */
    void registerReceiver(@NotNull EndpointDataReferenceReceiver receiver);

    /**
     * Apply all {@link EndpointDataReferenceReceiver} to the provided {@link EndpointDataReference}.
     */
    @NotNull
    CompletableFuture<Result<Void>> receiveAll(@NotNull EndpointDataReference edr);
}
