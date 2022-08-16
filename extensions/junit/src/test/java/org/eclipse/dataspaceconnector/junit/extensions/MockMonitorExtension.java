/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.junit.extensions;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.MonitorExtension;

import static org.mockito.Mockito.mock;

public class MockMonitorExtension implements MonitorExtension {
    private final Monitor monitorMock;

    public MockMonitorExtension() {
        monitorMock = mock(Monitor.class);
    }

    @Override
    public Monitor getMonitor() {
        return monitorMock;
    }
}
