/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.monitor.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;


public class OtelMonitor extends ConsoleMonitor {

    private final Supplier<Logger> otelLogger;

    public OtelMonitor(Level level) {
        this(level, GlobalOpenTelemetry::get);
    }

    public OtelMonitor(Monitor.Level level, Supplier<OpenTelemetry> openTelemetry) {
        super(level, true);
        this.otelLogger = () -> openTelemetry.get()
                .getLogsBridge()
                .loggerBuilder("org.eclipse.edc")
                .build();
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        super.severe(supplier, errors);
        emit(Severity.ERROR, supplier, errors);
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        super.warning(supplier, errors);
        emit(Severity.WARN, supplier, errors);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        super.info(supplier, errors);
        emit(Severity.INFO, supplier, errors);
    }


    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        super.debug(supplier, errors);
        emit(Severity.DEBUG, supplier, errors);
    }


    private void emit(Severity severity, Supplier<String> supplier, Throwable... errors) {
        var builder = otelLogger.get().logRecordBuilder()
                .setSeverity(severity)
                .setBody(sanitizeMessage(supplier));

        if (errors != null) {
            for (var error : errors) {
                if (error != null) {
                    // OTel semantic conventions for exceptions
                    builder.setAttribute(AttributeKey.stringKey("exception.type"), error.getClass().getName())
                            .setAttribute(AttributeKey.stringKey("exception.message"), error.getMessage())
                            .setAttribute(AttributeKey.stringKey("exception.stacktrace"), stackTrace(error));
                }
            }
        }
        builder.emit();
    }

    private static String stackTrace(Throwable t) {
        var sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
