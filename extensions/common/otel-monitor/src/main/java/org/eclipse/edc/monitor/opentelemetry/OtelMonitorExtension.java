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

package org.eclipse.edc.monitor.opentelemetry;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;

import static org.eclipse.edc.monitor.opentelemetry.OtelMonitorExtension.NAME;

@Extension(value = NAME)
public class OtelMonitorExtension implements MonitorExtension {
    public static final String NAME = "OpenTelemetry Monitor Extension";

    @Override
    public Monitor getMonitor(Monitor.Level level, String[] programArgs) {
        return new OtelMonitor(level);
    }
}
