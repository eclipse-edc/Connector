package org.eclipse.dataspaceconnector.spi.telemetry;

import java.util.Map;

public interface TraceCarrier {

    Map<String, String> getTraceContext();

}
