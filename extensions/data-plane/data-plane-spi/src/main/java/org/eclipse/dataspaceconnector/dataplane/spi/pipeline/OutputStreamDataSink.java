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
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Sends data to an output stream. The transfer is done asynchronously using the supplied executor service.
 */
public class OutputStreamDataSink implements DataSink {
    private final OutputStream stream;
    private final ExecutorService executorService;
    private final Monitor monitor;

    public OutputStreamDataSink(OutputStream stream, ExecutorService executorService, Monitor monitor) {
        this.stream = stream;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @Override
    public CompletableFuture<TransferResult> transfer(DataSource source) {
        try (var partStream = source.openPartStream()) {
            var futures = partStream.map(part -> supplyAsync(() -> transferData(part), executorService)).collect(toList());
            return allOf(futures.toArray(CompletableFuture[]::new)).thenApply((s) -> {
                if (futures.stream().anyMatch(future -> future.getNow(null).failed())) {
                    return TransferResult.failure(ERROR_RETRY, "Error transferring data");
                }
                return TransferResult.success();
            });
        } catch (Exception e) {
            monitor.severe("Error processing data transfer request", e);
            return CompletableFuture.completedFuture(TransferResult.failure(ERROR_RETRY, "Error processing data transfer request"));
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
