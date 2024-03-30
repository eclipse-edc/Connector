/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system.testextensions;

import org.eclipse.edc.boot.system.DependencyGraph;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.mockito.Mockito.mock;

public class BaseRuntimeFixture extends BaseRuntime {

    private final Monitor monitor;
    private final List<ServiceExtension> extensions;

    private final HealthCheckService healthCheckService = mock(HealthCheckService.class);

    public BaseRuntimeFixture(Monitor monitor, List<ServiceExtension> extensions) {
        this.monitor = monitor;
        this.extensions = extensions;
    }

    public void start() {
        bootWithoutShutdownHook();
    }

    public void stop() {
        super.shutdown();
    }

    @Override
    protected void exit() {
        // do nothing
    }

    @Override
    protected List<InjectionContainer<ServiceExtension>> createExtensions(ServiceExtensionContext context) {

        if (extensions != null && !extensions.isEmpty()) {
            return new DependencyGraph(context).of(extensions);
        } else {
            return super.createExtensions(context);
        }
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(Monitor monitor) {
        var ctx = super.createContext(monitor);
        ctx.registerService(HealthCheckService.class, healthCheckService);
        return ctx;
    }

    @Override
    protected @NotNull Monitor createMonitor() {
        return monitor;
    }
}
