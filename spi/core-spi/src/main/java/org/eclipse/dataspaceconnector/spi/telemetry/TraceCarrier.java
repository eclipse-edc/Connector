package org.eclipse.dataspaceconnector.spi.telemetry;

import java.util.Map;

/**
 * Interface for trace context carrier entities.
 *
 * Use in combination with {@link Telemetry#setCurrentTraceContext(TraceCarrier)} to propagate the tracing context stored in the entity to the current thread.
 */
public interface TraceCarrier {

    Map<String, String> getTraceContext();

}
