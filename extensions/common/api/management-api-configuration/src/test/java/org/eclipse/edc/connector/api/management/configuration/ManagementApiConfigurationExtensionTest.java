/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.configuration;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.DEPRECATED_SETTINGS;
import static org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension.SETTINGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ManagementApiConfigurationExtensionTest {

    private ManagementApiConfigurationExtension extension;
    private final WebServiceConfigurer configurer = mock(WebServiceConfigurer.class);
    private final Monitor monitor = mock(Monitor.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(WebServer.class, mock(WebServer.class));
        context.registerService(WebService.class, mock(WebService.class));
        context.registerService(WebServiceConfigurer.class, configurer);
        extension = factory.constructInstance(ManagementApiConfigurationExtension.class);
    }

    @Test
    void initialize_shouldUseNewManagementConfigName_whenTheOldOneIsNotDefinedInConfig() {
        var context = contextWithConfig(ConfigFactory.empty());
        when(configurer.configure(any(), any(), any())).thenReturn(mock(WebServiceConfiguration.class));

        extension.initialize(context);

        verify(configurer).configure(any(), any(), eq(SETTINGS));
    }

    @Test
    void initialize_shouldUseDeprecatedConfigName_whenItsDefinedAtThePlaceOfTheCurrentOne() {
        var config = Map.of("web.http.data.port", "8181");
        var context = contextWithConfig(ConfigFactory.fromMap(config));
        when(configurer.configure(any(), any(), any())).thenReturn(mock(WebServiceConfiguration.class));

        extension.initialize(context);

        verify(configurer).configure(any(), any(), eq(DEPRECATED_SETTINGS));
        verify(monitor).warning(anyString());
    }

    @Test
    void initialize_shouldUseCurrentConfig_whenBothCurrentAndDeprecatedAreDefined() {
        var config = Map.of("web.http.data.port", "8181", "web.http.management.port", "8181");
        var context = contextWithConfig(ConfigFactory.fromMap(config));
        when(configurer.configure(any(), any(), any())).thenReturn(mock(WebServiceConfiguration.class));

        extension.initialize(context);

        verify(configurer).configure(any(), any(), eq(SETTINGS));
    }

    @NotNull
    private DefaultServiceExtensionContext contextWithConfig(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, mock(Telemetry.class), List.of(() -> config));
        context.initialize();
        return context;
    }
}
