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
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

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

    public static final String TYPE = "AsyncStreaming";

    private final AsyncResponseContext asyncContext;
    private final ExecutorService executorService;

    public AsyncStreamingDataSink(AsyncResponseContext asyncContext, ExecutorService executorService) {
        this.asyncContext = asyncContext;
        this.executorService = executorService;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        var streamResult = source.openPartStream();
        if (streamResult.failed()) {
            return completedFuture(failure(streamResult.getFailure()));
        }

        try (var partStream = streamResult.getContent()) {
            return partStream
                    .map(part -> supplyAsync(() -> transferPart(part), executorService))
                    .collect(asyncAllOf())
                    .thenApply(this::processResults);
        }
    }

    @NotNull
    private StreamResult<Object> processResults(List<? extends StatusResult<?>> results) {
        if (results.stream().anyMatch(AbstractResult::failed)) {
            return error("Error transferring data");
        }
        return success();
    }

    @NotNull
    private StatusResult<?> transferPart(DataSource.Part part) {
        var result = asyncContext.register(new AsyncResponseCallback((outputStream) -> {
            try {
                part.openStream().transferTo(outputStream);
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }, part.mediaType()));

        return result ? StatusResult.success() : failure(FATAL_ERROR, "Could not resume output stream write");
    }

    /**
     * Serves as a facade for a response context that writes data asynchronously to a client.
     */
    @FunctionalInterface
    public interface AsyncResponseContext {

        /**
         * Registers a callback when an output stream is available for writing data. The second parameter is the media type.
         *
         * @param callback the callback
         * @return true if the callback was successfully registered
         */
        boolean register(AsyncResponseCallback callback);
    }

    public record AsyncResponseCallback(Consumer<OutputStream> outputStreamConsumer, String mediaType) {

    }

}
