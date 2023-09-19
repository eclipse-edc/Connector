/*
 *  Copyright (c) 2023 NTT DATA Group Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       NTT DATA Group Corporation - initial implementation
 *
 */

package org.eclipse.edc.monitor.slf4j;

import org.eclipse.edc.junit.extension.MockMonitorExtension;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
class Slf4jBridgeExtensionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jBridgeExtensionTest.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        var mockMonitorExtension = new MockMonitorExtension();
        extension.registerSystemExtension(MonitorExtension.class, mockMonitorExtension);
    }

    @Test
    void testLogForwarded(EdcExtension extension) {
        var mockedMonitor = extension.getContext().getMonitor();
        LOGGER.info("info by slf4j API");
        verify(mockedMonitor, never()).info(startsWith("info by slf4j API"), any());
        LOGGER.warn("warn by slf4j API");
        verify(mockedMonitor, atLeastOnce()).info(startsWith("warn by slf4j API"), any());
        LOGGER.error("error by slf4j API");
        verify(mockedMonitor, atLeastOnce()).severe(startsWith("error by slf4j API"), any());
    }
}
