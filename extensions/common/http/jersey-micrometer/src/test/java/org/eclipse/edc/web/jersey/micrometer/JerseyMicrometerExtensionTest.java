/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.eclipse.edc.web.jersey.micrometer.JerseyMicrometerExtension.ENABLE_JERSEY_METRICS;
import static org.eclipse.edc.web.jersey.micrometer.JerseyMicrometerExtension.ENABLE_METRICS;
import static org.eclipse.edc.web.jersey.micrometer.JerseyMicrometerExtension.JERSEY_METRICS_CONTEXTS;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class JerseyMicrometerExtensionTest {

    private final WebService webService = mock();
    private final PortMappingRegistry portMappingRegistry = mock();
    private final Monitor monitor = mock();
    private final ServiceExtensionContext extensionContext = mock();
    private JerseyMicrometerExtension extension;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(WebService.class, webService);
        context.registerService(MeterRegistry.class, new SimpleMeterRegistry());
        context.registerService(PortMappingRegistry.class, portMappingRegistry);
        extension = factory.constructInstance(JerseyMicrometerExtension.class);

        when(portMappingRegistry.getAll()).thenReturn(List.of(
                new PortMapping("default", 8181, "/api"),
                new PortMapping("management", 8182, "/api/management"),
                new PortMapping("protocol", 8183, "/api/dsp")));
        when(extensionContext.getMonitor()).thenReturn(monitor);
        when(extensionContext.getSetting(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
    }

    @Test
    void shouldMeasureDefaultContext_whenNotConfigured() {
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("default");

        extension.initialize(extensionContext);
        extension.prepare();

        verify(webService).registerResource(eq("default"), isA(MetricsApplicationEventListener.class));
        verifyNoMoreInteractions(webService);
    }

    @Test
    void shouldMeasureConfiguredContexts() {
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("management, protocol");

        extension.initialize(extensionContext);
        extension.prepare();

        verify(webService).registerResource(eq("management"), isA(MetricsApplicationEventListener.class));
        verify(webService).registerResource(eq("protocol"), isA(MetricsApplicationEventListener.class));
        verifyNoMoreInteractions(webService);
    }

    @Test
    void shouldMeasureAllContexts_whenWildcard() {
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("*");

        extension.initialize(extensionContext);
        extension.prepare();

        verify(webService).registerResource(eq("default"), isA(MetricsApplicationEventListener.class));
        verify(webService).registerResource(eq("management"), isA(MetricsApplicationEventListener.class));
        verify(webService).registerResource(eq("protocol"), isA(MetricsApplicationEventListener.class));
        verifyNoMoreInteractions(webService);
    }

    @Test
    void shouldSkipAndWarn_whenContextIsUnknown() {
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("management,unknown");

        extension.initialize(extensionContext);
        extension.prepare();

        verify(webService).registerResource(eq("management"), isA(MetricsApplicationEventListener.class));
        verifyNoMoreInteractions(webService);
        verify(monitor).warning(contains("unknown"));
    }

    @Test
    void shouldNotMeasure_whenJerseyMetricsDisabled() {
        when(extensionContext.getSetting(ENABLE_JERSEY_METRICS, true)).thenReturn(false);
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("*");

        extension.initialize(extensionContext);
        extension.prepare();

        verifyNoInteractions(webService);
    }

    @Test
    void shouldNotMeasure_whenMetricsDisabled() {
        when(extensionContext.getSetting(ENABLE_METRICS, true)).thenReturn(false);
        when(extensionContext.getSetting(JERSEY_METRICS_CONTEXTS, "default")).thenReturn("*");

        extension.initialize(extensionContext);
        extension.prepare();

        verifyNoInteractions(webService);
    }
}
