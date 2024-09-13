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

import org.eclipse.edc.boot.config.ConfigurationLoader;
import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.boot.system.testextensions.BaseExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
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
    private final ConfigurationLoader configurationLoader = mock();
    private final BaseRuntime runtime = new BaseRuntimeFixture(monitor, serviceLocator);

    @NotNull
    private static ServiceExtension registerService(Class<HealthCheckService> serviceClass, HealthCheckService healthCheckService) {
        return new ServiceExtension() {
            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(serviceClass, healthCheckService);
            }
        };
    }

    @Test
    void baseRuntime_shouldBoot() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(List.of(new BaseExtension()));

        runtime.boot(true);

        verify(monitor, never()).severe(anyString(), any());
    }

    @Test
    void baseRuntime_shouldNotBootWithException() {
        var extension = spy(new BaseExtension());

        doThrow(new EdcException("Failed to start base extension")).when(extension).start();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(List.of(extension));

        assertThatThrownBy(() -> runtime.boot(true)).isInstanceOf(EdcException.class);
        verify(monitor).severe(startsWith("Error booting runtime: Failed to start base extension"), any(EdcException.class));
    }

    @Test
    void shouldSetStartupCheckProvider_whenHealthCheckServiceIsRegistered() {
        var healthCheckService = mock(HealthCheckService.class);
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(List.of(
                new BaseExtension(), registerService(HealthCheckService.class, healthCheckService)));

        runtime.boot(true);

        verify(healthCheckService).addStartupStatusProvider(any());
    }

    @Test
    void shouldLoadConfiguration() {
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(List.of(new BaseExtension()));

        runtime.boot(true);

        verify(serviceLocator).loadImplementors(ConfigurationExtension.class, false);
    }

    @Test
    void configCanChangeParamArgs() {

        var config = mock(Config.class);
        when(config.getString(ConsoleMonitor.LOG_LEVEL_CONFIG, null)).thenReturn("INFO");

        var consoleMonitor = mock(ConsoleMonitor.class);

        var args = runtime.setConsoleMonitorLogLevelFromConfig(config, consoleMonitor, new String[0]);

        assertThat(args[0]).isEqualTo(String.format("%s=INFO", ConsoleMonitor.LEVEL_PROG_ARG));
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
