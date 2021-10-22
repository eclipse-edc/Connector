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

package org.eclipse.dataspaceconnector.monitor;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.system.ExtensionLoader;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Bridges from SLF4J to a monitor.
 */
public class MonitorProvider implements SLF4JServiceProvider {
    private static Monitor INSTANCE = ExtensionLoader.loadMonitor();

    public static void setInstance(Monitor monitor) {
        INSTANCE = monitor;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return (s) -> new MonitorLogger();
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return null;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return null;
    }

    @Override
    public String getRequesteApiVersion() {
        return "1.8";
    }

    @Override
    public void initialize() {
    }

    private static class MonitorLogger extends AbstractLogger {

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return true;
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
            switch (level) {
                case ERROR:
                    INSTANCE.severe(msg, throwable);
                    break;
                default:
                case INFO:
                    INSTANCE.info(msg, throwable);
                    break;
                case DEBUG:
                    INSTANCE.debug(msg, throwable);
                    break;
            }
        }
    }
}
