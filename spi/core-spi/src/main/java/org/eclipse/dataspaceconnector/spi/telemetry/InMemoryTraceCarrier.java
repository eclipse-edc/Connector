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

import java.util.Collections;
import java.util.Map;

/**
 * Simple {@link TraceCarrier} to use in situations where no entity is persisted (e.g. asynchronous processing)
 */
class InMemoryTraceCarrier implements TraceCarrier {

    private final Map<String, String> traceContext;

    InMemoryTraceCarrier(Map<String, String> traceContext) {
        this.traceContext = Collections.unmodifiableMap(traceContext);
    }

    @Override
    public Map<String, String> getTraceContext() {
        return traceContext;
    }
}
