package com.microsoft.dagx.monitor;

import com.microsoft.dagx.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Default monitor implementation.
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
        output("SEVERE", supplier);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        if (Level.INFO.value < level.value) {
            return;
        }
        output("INFO", supplier);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        if (Level.DEBUG.value < level.value) {
            return;
        }
        output("DEBUG", supplier);
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
