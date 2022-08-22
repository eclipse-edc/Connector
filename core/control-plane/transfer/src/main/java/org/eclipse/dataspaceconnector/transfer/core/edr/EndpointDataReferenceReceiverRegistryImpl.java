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

package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.common.async.AsyncUtils.asyncAllOf;

/**
 * In-memory implementation of {@link EndpointDataReferenceReceiverRegistry}.
 */
public class EndpointDataReferenceReceiverRegistryImpl implements EndpointDataReferenceReceiverRegistry {

    private final List<EndpointDataReferenceReceiver> receivers = new ArrayList<>();

    @Override
    public void registerReceiver(@NotNull EndpointDataReferenceReceiver receiver) {
        receivers.add(receiver);
    }

    @Override
    public @NotNull CompletableFuture<Result<Void>> receiveAll(@NotNull EndpointDataReference edr) {
        return receivers.stream()
                .map(receiver -> receiver.send(edr))
                .collect(asyncAllOf())
                .thenApply(results -> results.stream()
                        .filter(Result::failed)
                        .findFirst()
                        .map(failed -> Result.<Void>failure(failed.getFailureMessages()))
                        .orElse(Result.success()))
                .exceptionally(throwable -> Result.failure("Failed to receive endpoint data reference: " + throwable.getMessage()));
    }
}
