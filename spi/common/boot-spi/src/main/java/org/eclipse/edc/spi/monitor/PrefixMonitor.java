/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH -  initial implementation
 *
 */

package org.eclipse.edc.spi.monitor;

import java.util.function.Supplier;

/**
 * Monitor implementation which will prefix a supplied text to all log messages.
 * Note that this monitor will delegate the actual log execution to the underlying monitor provided by the context.
 */
class PrefixMonitor implements Monitor {

    private static final String MESSAGE_FORMAT = "[%s] %s";

    private final Monitor monitor;
    private final String prefix;

    PrefixMonitor(Monitor monitor, String prefix) {
        this.monitor = monitor;
        this.prefix = prefix;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        monitor.severe(() -> MESSAGE_FORMAT.formatted(prefix, supplier.get()), errors);
    }

    @Override
    public void severe(String message, Throwable... errors) {
        monitor.severe(MESSAGE_FORMAT.formatted(prefix, message), errors);
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        monitor.warning(() -> MESSAGE_FORMAT.formatted(prefix, supplier.get()), errors);
    }

    @Override
    public void warning(String message, Throwable... errors) {
        monitor.warning(MESSAGE_FORMAT.formatted(prefix, message), errors);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        monitor.info(() -> MESSAGE_FORMAT.formatted(prefix, supplier.get()), errors);
    }

    @Override
    public void info(String message, Throwable... errors) {
        monitor.info(MESSAGE_FORMAT.formatted(prefix, message), errors);
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        monitor.debug(() -> MESSAGE_FORMAT.formatted(prefix, supplier.get()), errors);
    }

    @Override
    public void debug(String message, Throwable... errors) {
        monitor.debug(MESSAGE_FORMAT.formatted(prefix, message), errors);
    }
}
