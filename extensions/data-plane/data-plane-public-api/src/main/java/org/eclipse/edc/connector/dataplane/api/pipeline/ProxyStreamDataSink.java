/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.api.pipeline;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;

/**
 * Stream directly the data source to the caller. Note: it will work only on the first "part", because this works only
 * in a proxy context.
 */
public class ProxyStreamDataSink implements DataSink {
    private final String requestId;
    private final Monitor monitor;

    public ProxyStreamDataSink(String requestId, Monitor monitor) {
        this.requestId = requestId;
        this.monitor = monitor;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        var streamResult = source.openPartStream();
        if (streamResult.failed()) {
            return completedFuture(failure(streamResult.getFailure()));
        }

        try (var partStream = streamResult.getContent()) {

            return partStream.findFirst()
                    .map(part -> new ProxyStreamPayload(part.openStream(), part.mediaType()))
                    .map(StreamResult::<Object>success)
                    .map(CompletableFuture::completedFuture)
                    .orElse(CompletableFuture.failedFuture(new EdcException("no parts")));
        } catch (Exception e) {
            var errorMessage = format("Error processing data transfer request - Request ID: %s", requestId);
            monitor.severe(errorMessage, e);
            return CompletableFuture.completedFuture(error(errorMessage));
        }
    }
}
