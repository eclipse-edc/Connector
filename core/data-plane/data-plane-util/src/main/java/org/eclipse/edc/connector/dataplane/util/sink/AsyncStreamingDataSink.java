/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.response.StatusResult.failure;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

/**
 * Asynchronously streams data to a response client.
 */
public class AsyncStreamingDataSink implements DataSink {

    /**
     * Serves as a facade for a response context that writes data asynchronously to a client.
     */
    @FunctionalInterface
    public interface AsyncResponseContext {

        /**
         * Registers a callback when an output stream is available for writing data.
         *
         * @param consumer the callback
         * @return true if the callback was successfully registered
         */
        boolean register(Consumer<OutputStream> consumer);
    }

    private final AsyncResponseContext asyncContext;
    private final ExecutorService executorService;
    private final Monitor monitor;

    public AsyncStreamingDataSink(AsyncResponseContext asyncContext, ExecutorService executorService, Monitor monitor) {
        this.asyncContext = asyncContext;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        var streamResult = source.openPartStream();
        if (streamResult.failed()) {
            return completedFuture(failure(streamResult.getFailure()));
        }
        var partStream = streamResult.getContent();
        return partStream
                .map(part -> supplyAsync(() -> transferPart(part), executorService))
                .collect(asyncAllOf())
                .thenApply(r -> processResults(r, partStream));
    }

    @NotNull
    private StreamResult<Object> processResults(List<? extends StatusResult<?>> results, Stream<DataSource.Part> partStream) {
        close(partStream);
        if (results.stream().anyMatch(AbstractResult::failed)) {
            return error("Error transferring data");
        }
        return success();
    }

    @NotNull
    private StatusResult<?> transferPart(DataSource.Part part) {
        var result = asyncContext.register(outputStream -> {
            try {
                part.openStream().transferTo(outputStream);
            } catch (IOException e) {
                throw new EdcException(e);
            }
        });
        return result ? StatusResult.success() : failure(FATAL_ERROR, "Could not resume output stream write");
    }

    private void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            monitor.warning("Error closing stream", e);
        }
    }

}
