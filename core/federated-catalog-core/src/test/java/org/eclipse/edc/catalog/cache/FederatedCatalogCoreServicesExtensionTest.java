/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.catalog.crawler.RecurringExecutionPlan;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.crawler.spi.model.ExecutionPlan;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class FederatedCatalogCoreServicesExtensionTest {

    private final FederatedCatalogCache store = mock();
    private final TargetNodeDirectory nodeDirectory = mock();
    private final HealthCheckService healthCheckService = mock();
    private final ExecutionPlan executionPlan = mock();
    private FederatedCatalogCoreServicesExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        var monitorWithPrefix = mock(Monitor.class);
        var monitor = mock(Monitor.class);
        when(monitor.withPrefix(anyString())).thenReturn(monitorWithPrefix);

        context.registerService(TargetNodeDirectory.class, nodeDirectory);
        context.registerService(FederatedCatalogCache.class, store);
        context.registerService(TargetNodeFilter.class, null);
        context.registerService(ExecutionPlan.class, new RecurringExecutionPlan(Duration.ofSeconds(1), Duration.ofSeconds(0), mock()));
        context.registerService(Monitor.class, monitor);
        context.registerService(HealthCheckService.class, healthCheckService);
        context.registerService(ExecutionPlan.class, executionPlan);

        extension = factory.constructInstance(FederatedCatalogCoreServicesExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        extension.initialize(context);

        verify(context, atLeastOnce()).getMonitor();
    }

    @Test
    void initialize_withHealthCheck(ServiceExtensionContext context, FederatedCatalogCoreServicesExtension extension) {
        extension.initialize(context);

        verify(healthCheckService).addReadinessProvider(any());
    }

    @Test
    void initialize_withDisabledExecution(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var mockedConfig = ConfigFactory.fromMap(Map.of("edc.catalog.cache.execution.enabled", Boolean.FALSE.toString()));
        when(context.getConfig()).thenReturn(mockedConfig);

        var extension = objectFactory.constructInstance(FederatedCatalogCoreServicesExtension.class);

        extension.initialize(context);
        extension.start();

        verifyNoInteractions(executionPlan);
    }

    @Test
    void start(ServiceExtensionContext context) {
        extension.initialize(context);
    }


}
