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

package org.eclipse.dataspaceconnector.spi.monitor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Delegates to multiple monitor implementations. This can be used to output messages to multiple destinations.
 */
public class MultiplexingMonitor implements Monitor {

    private final Collection<Monitor> internalMonitors;

    public MultiplexingMonitor(List<Monitor> monitors) {
        internalMonitors = monitors;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.severe(supplier, errors));
    }

    @Override
    public void severe(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.severe(message, errors));
    }

    @Override
    public void severe(Map<String, Object> data) {
        internalMonitors.forEach(m -> m.severe(data));
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.warning(supplier, errors));
    }

    @Override
    public void warning(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.warning(message, errors));
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.info(supplier, errors));
    }

    @Override
    public void info(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.info(message, errors));
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.debug(supplier, errors));
    }

    @Override
    public void debug(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.debug(message, errors));
    }
}
