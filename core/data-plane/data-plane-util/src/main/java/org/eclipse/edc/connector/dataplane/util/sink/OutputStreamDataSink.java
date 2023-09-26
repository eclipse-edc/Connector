/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

/**
 * Sends data to an output stream. The transfer is done asynchronously using the supplied executor service.
 */
public class OutputStreamDataSink implements DataSink {
    private final String requestId;
    private final OutputStream stream;
    private final ExecutorService executorService;
    private final Monitor monitor;

    public OutputStreamDataSink(String requestId, ExecutorService executorService, Monitor monitor) {
        this.requestId = requestId;
        this.stream = new ByteArrayOutputStream();
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        var streamResult = source.openPartStream();
        if (streamResult.failed()) {
            return completedFuture(failure(streamResult.getFailure()));
        }

        try (var partStream = streamResult.getContent()) {
            return partStream
                    .map(part -> supplyAsync(() -> transferData(part), executorService))
                    .collect(asyncAllOf())
                    .thenApply(results -> {
                        if (results.stream().anyMatch(AbstractResult::failed)) {
                            return error("Error transferring data");
                        }
                        return StreamResult.success(stream.toString());
                    });
        } catch (Exception e) {
            var errorMessage = format("Error processing data transfer request - Request ID: %s", requestId);
            monitor.severe(errorMessage, e);
            return CompletableFuture.completedFuture(error(errorMessage));
        }
    }

    private Result<Void> transferData(DataSource.Part part) {
        try (var source = part.openStream()) {
            source.transferTo(stream);
            return Result.success();
        } catch (Exception e) {
            monitor.severe("Error writing data", e);
            return Result.failure("Error writing data");
        }
    }
}
