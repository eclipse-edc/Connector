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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.telemetry.TraceCarrier;
import org.eclipse.edc.util.stream.PartitionIterator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

/**
 * Writes data in parallel.
 */
public abstract class ParallelSink implements DataSink {
    protected String requestId;
    protected int partitionSize = 5;
    protected ExecutorService executorService;
    protected Monitor monitor;
    protected Telemetry telemetry;

    @WithSpan
    @Override
    public CompletableFuture<StreamResult<Void>> transfer(DataSource source) {
        try {
            var streamResult = source.openPartStream();
            if (streamResult.failed()) {
                return completedFuture(failure(streamResult.getFailure()));
            }

            try (var partStream = streamResult.getContent()) {
                var partitioned = PartitionIterator.streamOf(partStream, partitionSize);
                var traceCarrier = telemetry.getTraceCarrierWithCurrentContext();

                var futures = partitioned.map(parts -> processPartsAsync(parts, traceCarrier)).collect(toList());
                return futures.stream()
                        .collect(asyncAllOf())
                        .thenApply(results -> results.stream()
                                .filter(AbstractResult::failed)
                                .findFirst()
                                .map(r -> StreamResult.<Void>error(String.join(",", r.getFailureMessages())))
                                .orElseGet(this::complete))
                        .exceptionally(throwable -> StreamResult.error("Unhandled exception raised when transferring data: " + throwable.getMessage()));
            }
        } catch (Exception e) {
            var errorMessage = format("Error processing data transfer request - Request ID: %s", requestId);
            monitor.severe(errorMessage, e);
            return CompletableFuture.completedFuture(StreamResult.error(errorMessage));
        }
    }

    @NotNull
    private CompletableFuture<StreamResult<Void>> processPartsAsync(List<DataSource.Part> parts, TraceCarrier traceCarrier) {
        Supplier<StreamResult<Void>> supplier = () -> transferParts(parts);
        return supplyAsync(telemetry.contextPropagationMiddleware(supplier, traceCarrier), executorService);
    }

    protected abstract StreamResult<Void> transferParts(List<DataSource.Part> parts);

    /**
     * Called after all parallel parts are transferred, only if all parts were successfully transferred.
     * <p>
     * Implementations may override this method to perform completion logic, such as writing a completion marker.
     *
     * @return status result to be returned to caller.
     */
    protected StreamResult<Void> complete() {
        return StreamResult.success();
    }

    protected abstract static class Builder<B extends Builder<B, T>, T extends ParallelSink> {
        protected T sink;

        protected Builder(T sink) {
            this.sink = sink;
            this.sink.telemetry = new Telemetry(); // default noop implementation
        }

        public B requestId(String requestId) {
            sink.requestId = requestId;
            return self();
        }

        public B partitionSize(int partitionSize) {
            sink.partitionSize = partitionSize;
            return self();
        }

        public B executorService(ExecutorService executorService) {
            sink.executorService = executorService;
            return self();
        }

        public B monitor(Monitor monitor) {
            sink.monitor = monitor;
            return self();
        }

        public B telemetry(Telemetry telemetry) {
            sink.telemetry = telemetry;
            return self();
        }

        public T build() {
            Objects.requireNonNull(sink.requestId, "requestId");
            Objects.requireNonNull(sink.executorService, "executorService");
            validate();
            return sink;
        }

        protected abstract void validate();

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
