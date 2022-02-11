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

package org.eclipse.dataspaceconnector.core.monitor;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Default monitor implementation. Outputs messages to the console.
 */
public class ConsoleMonitor implements Monitor {

    private static final String SEVERE = "SEVERE";
    private static final String WARNING = "WARNING";
    private static final String INFO = "INFO";
    private static final String DEBUG = "DEBUG";

    private final Level level;
    private final String prefix;

    public ConsoleMonitor() {
        this.prefix = "";
        this.level = Level.DEBUG;
    }

    public ConsoleMonitor(@Nullable String runtimeName, Level level) {
        this.prefix = format("[%s] ", runtimeName);
        this.level = level;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        output(SEVERE, supplier, errors);
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        if (Level.WARNING.value < level.value) {
            return;
        }
        output(WARNING, supplier, errors);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output(INFO, supplier, errors);
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output(DEBUG, supplier, errors);
    }

    private void output(String level, Supplier<String> supplier, Throwable... errors) {
        String time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println(prefix + level + " " + time + " " + supplier.get());
        if (errors != null) {
            for (Throwable error : errors) {
                if (error != null) {
                    error.printStackTrace(System.out);
                }
            }
        }
    }

    public enum Level {
        SEVERE(3), WARNING(2), INFO(1), DEBUG(0);

        private final int value;

        Level(int value) {
            this.value = value;
        }
    }
}
