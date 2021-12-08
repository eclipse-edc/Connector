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
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoreServicesExtensionTest {

    private final CoreServicesExtension extension = new CoreServicesExtension();

    @Test
    void provides() {
        assertThat(extension.provides()).containsExactlyInAnyOrder("dataspaceconnector:http-client", "edc:retry-policy");
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
        when(context.getMonitor()).thenReturn(new Monitor() {});
        when(context.getSetting(eq("edc.core.retry.retries.max"), anyString())).thenReturn("3");
        when(context.getSetting(eq("edc.core.retry.backoff.min"), anyString())).thenReturn("500");
        when(context.getSetting(eq("edc.core.retry.backoff.max"), anyString())).thenReturn("10000");
        doNothing().when(context).registerService(any(), any());
        when(context.getService(Vault.class)).thenReturn(mock(Vault.class));
        when(context.getService(eq(PrivateKeyResolver.class))).thenReturn(mock(PrivateKeyResolver.class));

        extension.initialize(context);

        verify(context).registerService(eq(OkHttpClient.class), isA(OkHttpClient.class));
        verify(context).registerService(eq(RetryPolicy.class), isA(RetryPolicy.class));
    }
}
