/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.boot.health;

import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HealthCheckServiceImplTest {

    private final HealthCheckResult.Builder statusBuilder = HealthCheckResult.Builder.newInstance().component("test status");
    private HealthCheckServiceImpl service;

    @BeforeEach
    void setup() {
        service = new HealthCheckServiceImpl();
    }

    @Test
    void isLive() {
        var lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenReturn(successResult());
        service.addLivenessProvider(lpm);
        assertThat(service.isLive().isHealthy()).isTrue();
        verify(lpm, atLeastOnce()).get();
        verifyNoMoreInteractions(lpm);
    }

    @Test
    void isLive_throwsException() {
        var lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenThrow(new RuntimeException("test exception"));
        service.addLivenessProvider(lpm);

        assertThat(service.isLive().isHealthy()).isFalse();
        verify(lpm, atLeastOnce()).get();
        verifyNoMoreInteractions(lpm);
    }

    @Test
    void isLive_failed() {
        var lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenReturn(failedResult());
        service.addLivenessProvider(lpm);

        assertThat(service.isLive().isHealthy()).isFalse();
        verify(lpm, atLeastOnce()).get();
        verifyNoMoreInteractions(lpm);
    }

    @Test
    void isReady() {
        var provider = mock(ReadinessProvider.class);
        when(provider.get()).thenReturn(successResult());
        service.addReadinessProvider(provider);

        assertThat(service.isReady().isHealthy()).isTrue();

        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);
    }

    @Test
    void isReady_throwsException() {
        var provider = mock(ReadinessProvider.class);
        when(provider.get()).thenThrow(new RuntimeException("test-exception"));
        service.addReadinessProvider(provider);

        assertThat(service.isReady().isHealthy()).isFalse();
        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);
    }

    @Test
    void isReady_failed() {
        var provider = mock(ReadinessProvider.class);
        when(provider.get()).thenReturn(failedResult());
        service.addReadinessProvider(provider);

        assertThat(service.isReady().isHealthy()).isFalse();
        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);
    }

    @Test
    void hasStartupFinished() {
        var provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenReturn(successResult());
        service.addStartupStatusProvider(provider);

        assertThat(service.getStartupStatus().isHealthy()).isTrue();

        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);

    }

    @Test
    void hasStartupFinished_throwsException() {
        var provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenThrow(new RuntimeException("test-exception"));
        service.addStartupStatusProvider(provider);

        assertThat(service.getStartupStatus().isHealthy()).isFalse();

        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);

    }

    @Test
    void hasStartupFinished_failed() {
        var provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenReturn(failedResult());
        service.addStartupStatusProvider(provider);

        assertThat(service.getStartupStatus().isHealthy()).isFalse();

        verify(provider, atLeastOnce()).get();
        verifyNoMoreInteractions(provider);
    }

    private HealthCheckResult failedResult() {
        return statusBuilder.failure("test-error").build();
    }

    private HealthCheckResult successResult() {
        return statusBuilder.success().build();
    }
}