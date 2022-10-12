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

package org.eclipse.dataspaceconnector.api.observability;

import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.health.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ObservabilityApiControllerTest {

    private ObservabilityApiController controller;
    private HealthCheckService healthCheckServiceMock;

    @BeforeEach
    void setUp() {
        healthCheckServiceMock = mock(HealthCheckService.class);
        controller = new ObservabilityApiController(healthCheckServiceMock);
    }

    @Test
    void checkHealth() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void checkHealth_mixedResults() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void checkHealth_noProviders() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus());

        // no provider = system not healthy
        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getLiveness() {
        when(healthCheckServiceMock.isLive()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckServiceMock, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getLiveness_mixedResults() {
        when(healthCheckServiceMock.isLive()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getLiveness_noProviders() {
        when(healthCheckServiceMock.isLive()).thenReturn(new HealthStatus());

        assertThat(controller.getLiveness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).isLive();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getReadiness() {
        when(healthCheckServiceMock.isReady()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckServiceMock, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getReadiness_mixedResults() {
        when(healthCheckServiceMock.isReady()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getReadiness_noProvider() {
        when(healthCheckServiceMock.isReady()).thenReturn(new HealthStatus());

        assertThat(controller.getReadiness()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).isReady();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getStartup() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success()));

        assertThat(controller.getStartup()).extracting(Response::getStatus).isEqualTo(200);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getStartup_mixedResults() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus(HealthCheckResult.success(), HealthCheckResult.failed("test failure")));

        assertThat(controller.getStartup()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }

    @Test
    void getStartup_noProviders() {
        when(healthCheckServiceMock.getStartupStatus()).thenReturn(new HealthStatus());

        // no provider = system not healthy
        assertThat(controller.checkHealth()).extracting(Response::getStatus).isEqualTo(503);

        verify(healthCheckServiceMock, times(1)).getStartupStatus();
        verifyNoMoreInteractions(healthCheckServiceMock);
    }
}