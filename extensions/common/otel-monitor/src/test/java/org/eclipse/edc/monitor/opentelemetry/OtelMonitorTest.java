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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.logs.LoggerProvider;
import org.eclipse.edc.spi.monitor.Monitor.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtelMonitorTest {

    private LogRecordBuilder builder;
    private OtelMonitor monitor;

    private OtelMonitor monitorWithLevel(Level level) {
        var openTelemetry = mock(OpenTelemetry.class);
        var logs = mock(LoggerProvider.class);
        var logger = mock(Logger.class);
        builder = mock(LogRecordBuilder.class);

        var loggerBuilder = mock(LoggerBuilder.class);
        when(openTelemetry.getLogsBridge()).thenReturn(logs);
        when(logs.loggerBuilder(anyString())).thenReturn(loggerBuilder);
        when(loggerBuilder.build()).thenReturn(logger);
        when(logger.logRecordBuilder()).thenReturn(builder);
        when(builder.setSeverity(any())).thenReturn(builder);
        when(builder.setBody(anyString())).thenReturn(builder);
        when(builder.setAttribute(any(), anyString())).thenReturn(builder);

        return new OtelMonitor(level, () -> openTelemetry);
    }

    @Nested
    class WhenLevelIsDebug {

        @BeforeEach
        void setUp() {
            monitor = monitorWithLevel(Level.DEBUG);
        }

        @Test
        void debug_shouldEmit() {
            monitor.debug(() -> "test");
            verify(builder).emit();
        }

        @Test
        void info_shouldEmit() {
            monitor.info(() -> "test");
            verify(builder).emit();
        }

        @Test
        void warning_shouldEmit() {
            monitor.warning(() -> "test");
            verify(builder).emit();
        }

        @Test
        void severe_shouldEmit() {
            monitor.severe(() -> "test");
            verify(builder).emit();
        }
    }

    @Nested
    class WhenLevelIsInfo {

        @BeforeEach
        void setUp() {
            monitor = monitorWithLevel(Level.INFO);
        }

        @Test
        void debug_shouldNotEmit() {
            monitor.debug(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void info_shouldEmit() {
            monitor.info(() -> "test");
            verify(builder).emit();
        }

        @Test
        void warning_shouldEmit() {
            monitor.warning(() -> "test");
            verify(builder).emit();
        }

        @Test
        void severe_shouldEmit() {
            monitor.severe(() -> "test");
            verify(builder).emit();
        }
    }

    @Nested
    class WhenLevelIsWarning {

        @BeforeEach
        void setUp() {
            monitor = monitorWithLevel(Level.WARNING);
        }

        @Test
        void debug_shouldNotEmit() {
            monitor.debug(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void info_shouldNotEmit() {
            monitor.info(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void warning_shouldEmit() {
            monitor.warning(() -> "test");
            verify(builder).emit();
        }

        @Test
        void severe_shouldEmit() {
            monitor.severe(() -> "test");
            verify(builder).emit();
        }
    }

    @Nested
    class WhenLevelIsSevere {

        @BeforeEach
        void setUp() {
            monitor = monitorWithLevel(Level.SEVERE);
        }

        @Test
        void debug_shouldNotEmit() {
            monitor.debug(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void info_shouldNotEmit() {
            monitor.info(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void warning_shouldNotEmit() {
            monitor.warning(() -> "test");
            verify(builder, never()).emit();
        }

        @Test
        void severe_shouldEmit() {
            monitor.severe(() -> "test");
            monitor.severe(() -> "test", new Exception("test"));
            verify(builder, times(2)).emit();
            verify(builder, times(3)).setAttribute(any(), any());
        }
    }
}
