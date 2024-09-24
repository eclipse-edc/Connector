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

package org.eclipse.edc.spi.monitor;

import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Default monitor implementation. Outputs messages to the console.
 */
public class ConsoleMonitor implements Monitor {

    private static final String SEVERE = "SEVERE";
    private static final String WARNING = "WARNING";
    private static final String INFO = "INFO";
    private static final String DEBUG = "DEBUG";

    public static final String LEVEL_PROG_ARG = "--log-level";
    public static final String COLOR_PROG_ARG = "--no-color";


    private final boolean useColor;

    private Level level;
    private final String prefix;

    public ConsoleMonitor() {
        this(Level.getDefaultLevel(), true);
    }

    public ConsoleMonitor(Level level, boolean useColor) {
        this(null, level, useColor);
    }

    public ConsoleMonitor(@Nullable String runtimeName, Level level, boolean useColor) {
        this.prefix = runtimeName == null ? "" : "[%s] ".formatted(runtimeName);
        this.level = level;
        this.useColor = useColor;
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
        var time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var colorCode = useColor ? getColorCode(level) : "";
        var resetCode = useColor ? ConsoleColor.RESET : "";

        System.out.println(colorCode + prefix + level + " " + time + " " + sanitizeMessage(supplier) + resetCode);
        if (errors != null) {
            for (var error : errors) {
                if (error != null) {
                    System.out.print(colorCode);
                    error.printStackTrace(System.out);
                    System.out.print(resetCode);
                }
            }
        }
    }

    private String getColorCode(String level) {
        return switch (level) {
            case SEVERE -> ConsoleColor.RED;
            case WARNING -> ConsoleColor.YELLOW;
            case INFO -> ConsoleColor.GREEN;
            case DEBUG -> ConsoleColor.BLUE;
            default -> "";
        };
    }

    public enum Level {
        SEVERE(3), WARNING(2), INFO(1), DEBUG(0);

        private final int value;

        Level(int value) {
            this.value = value;
        }

        public static Level getDefaultLevel() {
            return Level.DEBUG;
        }

    }
}
