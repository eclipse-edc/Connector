package com.microsoft.dagx.spi.monitor;

import java.util.Map;
import java.util.function.Supplier;

/**
 * System monitoring and logging interface.
 */
public interface Monitor {

    default void severe(Supplier<String> supplier, Throwable... errors) {
    }

    default void severe(String message, Throwable... errors) {
        severe(() -> message, errors);
    }

    default void severe(Map<String, Object> data) {
    }

    default void info(Supplier<String> supplier, Throwable... errors) {
    }

    default void info(String message, Throwable... errors) {
        info(() -> message, errors);
    }

    default void debug(Supplier<String> supplier, Throwable... errors) {
    }

}
