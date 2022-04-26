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
 *
 */

package org.eclipse.dataspaceconnector.spi.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * System observability interface for tracing and metrics
 */
public class Telemetry {

    private final OpenTelemetry openTelemetry;

    public Telemetry() {
        this.openTelemetry = OpenTelemetry.noop();
    }

    public Telemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Gets the trace context from the current thread
     *
     * @return The trace context as a Map
     */
    public Map<String, String> getCurrentTraceContext() {
        Map<String, String> traceContext = new HashMap<>();
        openTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), traceContext, Map::put);
        return traceContext;
    }

    /**
     * Returns a trace carrier object containing the trace context from the current thread
     *
     * @return The trace carrier
     */
    public TraceCarrier getTraceCarrierWithCurrentContext() {
        return new InMemoryTraceCarrier(getCurrentTraceContext());
    }

    /**
     * Wraps a function with a middleware to propagate the trace context present in the carrier to the executing thread
     *
     * @param delegate The wrapped function
     * @return The resulting function with the context propagation middleware
     */
    public <T extends TraceCarrier, V> Function<T, V> contextPropagationMiddleware(Function<T, V> delegate) {
        return (t) -> {
            try (Scope scope = propagateTraceContext(t)) {
                return delegate.apply(t);
            }
        };
    }

    /**
     * Wraps a consumer with a middleware to propagate the trace context present in the carrier to the executing thread
     *
     * @param delegate The wrapped consumer
     * @return The resulting function with the context propagation middleware
     */
    public <T extends TraceCarrier> Consumer<T> contextPropagationMiddleware(Consumer<T> delegate) {
        return (t) -> {
            try (Scope scope = propagateTraceContext(t)) {
                delegate.accept(t);
            }
        };
    }

    /**
     * Wraps a supplier with a middleware to propagate the trace context present in the carrier to the executing thread
     *
     * @param delegate The wrapped supplier
     * @param traceCarrier The trace carrier
     * @return The resulting function with the context propagation middleware
     */
    public <T> Supplier<T> contextPropagationMiddleware(Supplier<T> delegate, TraceCarrier traceCarrier) {
        return () -> {
            try (Scope scope = propagateTraceContext(traceCarrier)) {
                return delegate.get();
            }
        };
    }

    private Scope propagateTraceContext(TraceCarrier carrier) {
        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), carrier, new TraceCarrierTextMapGetter());
        return extractedContext.makeCurrent();
    }

    private static class TraceCarrierTextMapGetter implements TextMapGetter<TraceCarrier> {

        @Override
        public Iterable<String> keys(TraceCarrier carrier) {
            return carrier.getTraceContext().keySet();
        }

        @Override
        public String get(TraceCarrier carrier, String key) {
            return carrier.getTraceContext().get(key);
        }
    }
}
