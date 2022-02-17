package org.eclipse.dataspaceconnector.spi.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
