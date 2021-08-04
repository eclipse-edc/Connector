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
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Default monitor implementation. Outputs messages to the console.
 */
public class ConsoleMonitor implements Monitor {
    private Level level;

    public enum Level {
        SEVERE(2), INFO(1), DEBUG(0);

        int value;

        Level(int value) {
            this.value = value;
        }
    }

    public ConsoleMonitor() {
        level = Level.DEBUG;
    }

    public ConsoleMonitor(@Nullable String runtimeName, Level level) {
        this.level = level;
    }

    public void severe(Supplier<String> supplier, Throwable... errors) {
        output("SEVERE", supplier, errors);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", supplier, errors);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output("DEBUG", supplier, errors);
    }

    private void output(String level, Supplier<String> supplier, Throwable... errors) {
        String time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println(level + " " + time + " " + supplier.get());
        if (errors != null) {
            for (Throwable error : errors) {
                if (error != null) {
                    error.printStackTrace(System.out);
                }
            }
        }
    }
}
