/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Catena-X Consortium - initial API and implementation
 *
 */

package org.eclipse.edc.monitor.logger;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.MonitorExtension;

/**
 * Extension adding logging monitor.
 *
 * @deprecated will be removed soon.
 */
@Extension("Logger monitor")
@Deprecated(since = "0.15.0")
public class LoggerMonitorExtension implements MonitorExtension {

    @Override
    public String name() {
        return "DEPRECATED: Logger Monitor";
    }

    @Override
    public Monitor getMonitor() {
        return new LoggerMonitor();
    }
}
