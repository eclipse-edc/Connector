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

package org.eclipse.edc.api.observability;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ObservabilityApiControllerTest {

    private final HealthCheckService healthCheckService = mock(HealthCheckService.class);
    private ObservabilityApiController controller;

    @BeforeEach
    void setUp() {
        controller = new ObservabilityApiController(healthCheckService, false, mock(Monitor.class));
    }

    @Test
    void checkHealth() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void checkHealth_mixedResults() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void checkHealth_noProviders() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus());

        // no provider = system not healthy
        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getLiveness() {
        when(healthCheckService.isLive()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckService, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getLiveness_mixedResults() {
        when(healthCheckService.isLive()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getLiveness_noProviders() {
        when(healthCheckService.isLive()).thenReturn(new HealthStatus());

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getReadiness() {
        when(healthCheckService.isReady()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckService, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getReadiness_mixedResults() {
        when(healthCheckService.isReady()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getReadiness_noProvider() {
        when(healthCheckService.isReady()).thenReturn(new HealthStatus());

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getStartup() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getStartup()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getStartup_mixedResults() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getStartup()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void getStartup_noProviders() {
        when(healthCheckService.getStartupStatus()).thenReturn(new HealthStatus());

        // no provider = system not healthy
        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckService, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckService);
    }
}
