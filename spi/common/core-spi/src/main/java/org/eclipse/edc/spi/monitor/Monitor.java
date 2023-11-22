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
 *       Mercedes-Benz Tech Innovation GmbH - prefix monitoring
 *
 */

package org.eclipse.edc.spi.monitor;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * System monitoring and logging interface.
 */
@ExtensionPoint
public interface Monitor {

    default void severe(Supplier<String> supplier, Throwable... errors) {
    }

    default void severe(String message, Throwable... errors) {
        severe(() -> message, errors);
    }

    default void severe(Map<String, Object> data) {
    }


    default void warning(Supplier<String> supplier, Throwable... errors) {
    }

    default void warning(String message, Throwable... errors) {
        warning(() -> message, errors);
    }

    default void info(Supplier<String> supplier, Throwable... errors) {
    }

    default void info(String message, Throwable... errors) {
        info(() -> message, errors);
    }

    default void debug(Supplier<String> supplier, Throwable... errors) {
    }

    default void debug(String message, Throwable... errors) {
        debug(() -> message, errors);
    }

    default String sanitizeMessage(Supplier<String> supplier) {
        return Optional.ofNullable(supplier.get())
                .map(msg -> msg.replaceAll("([\\r\\n])", " "))
                .orElse(null);
    }

    /**
     * Creates a prefixed {@link Monitor} which will prepend the supplied prefix parameter to the actual log message
     * in the format of: <strong>[LOGLEVEL] [TIMESTAMP] [[PREFIX]] [MESSAGE]</strong>
     * <br>
     * Example output: <strong>INFO 2023-11-08T15:53:39.989025 [JerseyExtension]: Registered Web API context alias: protocol</strong>
     *
     * @param prefix string value to be prefixed
     * @return the prefixed monitor
     */
    default Monitor withPrefix(String prefix) {
        return new PrefixMonitor(this, prefix);
    }

}
