/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.monitor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;

import java.util.Map;
import java.util.function.Supplier;

/**
 * System observability interface.
 */
public interface Monitor {

    default void severe(Supplier<String> supplier, Throwable... errors) {
    }

    default void severe(String message, Throwable... errors) {
        severe(() -> message, errors);
    }

    default void severe(Map<String, Object> data) {
    }

    
    default void warning(Supplier<String> supplier, Throwable... errors) {
    }

    default void warning(String message, Throwable... errors) {
        warning(() -> message, errors);
    }

    default void info(Supplier<String> supplier, Throwable... errors) {
    }

    default void info(String message, Throwable... errors) {
        info(() -> message, errors);
    }

    default void debug(Supplier<String> supplier, Throwable... errors) {
    }

    default void debug(String message, Throwable... errors) {
        debug(() -> message, errors);
    }

    default Telemetry telemetry() {
        return new Telemetry(GlobalOpenTelemetry.get());
    }

}
