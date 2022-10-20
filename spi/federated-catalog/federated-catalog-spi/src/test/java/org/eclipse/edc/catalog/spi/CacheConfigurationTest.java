/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.catalog.spi;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheConfigurationTest {

    private CacheConfiguration configuration;
    private ServiceExtensionContext context;
    private Monitor monitorMock;

    @BeforeEach
    void setup() {
        monitorMock = mock(Monitor.class);
        context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(monitorMock);
        configuration = new CacheConfiguration(context);
    }

    @Test
    void getExecutionPlan_whenLowPeriod() {
        when(context.getSetting(eq(CacheConfiguration.EXECUTION_PLAN_PERIOD_SECONDS), anyInt())).thenReturn(9);

        configuration.getExecutionPlan();
        verify(monitorMock).warning(startsWith("An execution period of 9 seconds is very low "));
    }

    @Test
    void getNumCrawlers() {
        configuration.getNumCrawlers();
        verify(context).getSetting(CacheConfiguration.NUM_CRAWLER_SETTING, 2);
        when(context.getSetting(eq(CacheConfiguration.NUM_CRAWLER_SETTING), eq(2))).thenReturn(9);
        assertThat(configuration.getNumCrawlers()).isEqualTo(9);
    }
}