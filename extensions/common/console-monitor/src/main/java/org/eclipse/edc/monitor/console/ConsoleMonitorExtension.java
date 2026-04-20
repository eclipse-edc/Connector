/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.monitor.console;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;

import java.util.Set;

import static org.eclipse.edc.monitor.console.ConsoleMonitorExtension.NAME;
import static org.eclipse.edc.spi.monitor.ConsoleMonitor.NO_COLOR_PROG_ARG;

@Extension(value = NAME)
public class ConsoleMonitorExtension implements MonitorExtension {
    public static final String NAME = "Console Monitor Extension";

    @Override
    public Monitor getMonitor(Monitor.Level level, String[] programArgs) {
        return new ConsoleMonitor(level, !Set.of(programArgs).contains(NO_COLOR_PROG_ARG));
    }
}
