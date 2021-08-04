/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.monitor;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
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
    private static Monitor INSTANCE;

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
        protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
            switch (level) {
                case ERROR:
                    INSTANCE.severe(msg, throwable);
                    break;
                case WARN:
                case INFO:
                    INSTANCE.info(msg, throwable);
                    break;
                case DEBUG:
                    INSTANCE.debug(msg, throwable);
                    break;
            }
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

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
    }
}
