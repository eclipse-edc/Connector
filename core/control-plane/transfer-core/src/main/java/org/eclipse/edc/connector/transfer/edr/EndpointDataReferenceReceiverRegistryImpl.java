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

package org.eclipse.edc.connector.transfer.edr;

import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

/**
 * In-memory implementation of {@link EndpointDataReferenceReceiverRegistry}.
 */
public class EndpointDataReferenceReceiverRegistryImpl implements EndpointDataReferenceReceiverRegistry, EventSubscriber {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final List<EndpointDataReferenceReceiver> receivers = new ArrayList<>();

    public EndpointDataReferenceReceiverRegistryImpl(TypeTransformerRegistry typeTransformerRegistry) {
        this.typeTransformerRegistry = typeTransformerRegistry;
    }

    @Override
    public void registerReceiver(@NotNull EndpointDataReferenceReceiver receiver) {
        receivers.add(receiver);
    }

    @Override
    public @NotNull CompletableFuture<Result<Void>> receiveAll(@NotNull EndpointDataReference edr) {
        return sendEdr(edr);
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (event.getPayload() instanceof TransferProcessStarted) {
            var msg = (TransferProcessStarted) event.getPayload();
            if (msg.getDataAddress() != null) {
                var edr = typeTransformerRegistry.transform(msg.getDataAddress(), EndpointDataReference.class)
                        .orElseThrow(failure -> new EdcException(format("Failed to send EDR for transfer process %s with error %s", msg.getTransferProcessId(), failure.getFailureDetail())));
                
                sendEdr(edr).join().orElseThrow(failure -> new EdcException(failure.getFailureDetail()));
            }
        }
    }

    private CompletableFuture<Result<Void>> sendEdr(@NotNull EndpointDataReference edr) {
        if (receivers.isEmpty()) {
            return CompletableFuture.failedFuture(new EdcException("There are no registered receivers."));
        } else {
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
}
