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

package org.eclipse.dataspaceconnector.system;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.system.health.HealthCheckServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CoreServicesExtensionTest {

    private final CoreServicesExtension extension = new CoreServicesExtension();

    @Test
    void provides() {
        assertThat(extension.provides()).containsExactlyInAnyOrder("dataspaceconnector:http-client", "edc:retry-policy", HealthCheckService.FEATURE);
    }

    @Test
    void requires() {
        assertThat(extension.requires()).isEmpty();
    }

    @Test
    void phase() {
        assertThat(extension.phase()).isEqualTo(ServiceExtension.LoadPhase.PRIMORDIAL);
    }

    @Test
    void initialize() {
        ServiceExtensionContext context = mock(ServiceExtensionContext.class);

        when(context.getSetting(eq(CoreServicesExtension.MAX_RETRIES), anyString())).thenReturn("3");
        when(context.getSetting(eq(CoreServicesExtension.BACKOFF_MIN_MILLIS), anyString())).thenReturn("500");
        when(context.getSetting(eq(CoreServicesExtension.BACKOFF_MAX_MILLIS), anyString())).thenReturn("10000");
        when(context.getService(eq(PrivateKeyResolver.class))).thenReturn(mock(PrivateKeyResolver.class));

        when(context.getSetting(eq(CoreServicesExtension.LIVENESS_PERIOD_SECONDS_SETTING), anyString())).thenReturn("60");
        when(context.getSetting(eq(CoreServicesExtension.READINESS_PERIOD_SECONDS_SETTING), anyString())).thenReturn("60");
        when(context.getSetting(eq(CoreServicesExtension.STARTUP_PERIOD_SECONDS_SETTING), anyString())).thenReturn("60");
        when(context.getSetting(eq(CoreServicesExtension.THREADPOOL_SIZE_SETTING), anyString())).thenReturn("3");

        extension.initialize(context);

        verify(context).registerService(eq(OkHttpClient.class), isA(OkHttpClient.class));
        verify(context).registerService(eq(RetryPolicy.class), isA(RetryPolicy.class));
        verify(context, atLeastOnce()).getSetting(any(), anyString());
        verify(context).getService(eq(PrivateKeyResolver.class));
        verify(context).registerService(eq(HealthCheckService.class), isA(HealthCheckServiceImpl.class));
        verifyNoMoreInteractions(context);
    }
}
