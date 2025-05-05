/*
 *  Copyright (c) 2020 - 2024 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.boot.system.runtime;

import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseRuntimeTest {

    private final Monitor monitor = mock();
    private final ServiceLocator serviceLocator = mock();
    private final BaseRuntime runtime = new BaseRuntimeFixture(monitor, serviceLocator);

    @Test
    void shouldBoot() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf());

        runtime.boot(true);

        verify(monitor, never()).severe(anyString(), any());
    }

    @Test
    void shouldNotBootWithException() {
        var extension = spy(extensionThatRegisters(Object.class, "any"));

        doThrow(new EdcException("Failed to start base extension")).when(extension).start();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(extension));

        assertThatThrownBy(() -> runtime.boot(true)).isInstanceOf(EdcException.class);
        verify(monitor).severe(startsWith("Error booting runtime: Failed to start base extension"), any(EdcException.class));
    }

    @Test
    void shouldSetStartupCheckProvider_whenHealthCheckServiceIsRegistered() {
        var healthCheckService = mock(HealthCheckService.class);
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(extensionThatRegisters(HealthCheckService.class, healthCheckService)));

        runtime.boot(true);

        verify(healthCheckService).addStartupStatusProvider(any());
    }

    @Test
    void shouldLoadConfiguration() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf());

        runtime.boot(true);

        verify(serviceLocator).loadImplementors(ConfigurationExtension.class, false);
    }

    @Test
    void shouldShutdownAllExtensionsAndCleanup_whenOneThrowsException() {
        ServiceExtension successful = mock();
        ServiceExtension unsuccessful = mock();
        doThrow(new RuntimeException("shutdown error")).when(unsuccessful).shutdown();
        doThrow(new RuntimeException("shutdown error")).when(unsuccessful).cleanup();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean()))
                .thenReturn(mutableListOf(successful, unsuccessful));

        runtime.boot(true);

        assertThatNoException().isThrownBy(runtime::shutdown);
        verify(successful).shutdown();
        verify(successful).cleanup();
        verify(unsuccessful).shutdown();
        verify(unsuccessful).cleanup();
    }

    @NotNull
    private <T> ServiceExtension extensionThatRegisters(Class<T> serviceClass, T service) {
        return new ServiceExtension() {
            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(serviceClass, service);
            }
        };
    }

    private static class BaseRuntimeFixture extends BaseRuntime {

        private final Monitor monitor;

        BaseRuntimeFixture(Monitor monitor, ServiceLocator serviceLocator) {
            super(serviceLocator);
            this.monitor = monitor;
        }

        @Override
        protected @NotNull Monitor createMonitor() {
            return monitor;
        }
    }
}
