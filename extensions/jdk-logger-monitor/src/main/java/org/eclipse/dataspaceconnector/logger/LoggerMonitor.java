/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Catena-X Consortium - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.logger;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging monitor using java.util.logging.
 */
public class LoggerMonitor implements Monitor {

    /**
     * Global logger.
     */
    private static final Logger LOGGER = Logger.getLogger(LoggerMonitor.class.getName());

    @Override
    public void severe(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.SEVERE, errors);
    }

    @Override
    public void severe(final Map<String, Object> data) {
        data.forEach((key, value) -> LOGGER.log(Level.SEVERE, key, value));
    }

    @Override
    public void warning(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.WARNING, errors);
    }

    @Override
    public void info(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.INFO, errors);
    }

    @Override
    public void debug(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.FINE, errors);
    }

    private void log(final Supplier<String> supplier, final Level level, final Throwable... errors) {
        if (errors == null || errors.length == 0) {
            LOGGER.log(level, supplier);
        } else {
            String logMessage = Optional.ofNullable(supplier.get())
                    .map(msg -> msg.replaceAll("([\\r\\n])", " "))
                    .orElse(null);
            Arrays.stream(errors).forEach(error -> LOGGER.log(level, logMessage, error));
        }
    }
}