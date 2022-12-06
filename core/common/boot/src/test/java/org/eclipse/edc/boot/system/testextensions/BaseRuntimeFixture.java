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
import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class BaseRuntimeFixture extends BaseRuntime {


    private final Monitor monitor;
    private final List<ServiceExtension> extensions;

    private final HealthCheckService healthCheckService = mock(HealthCheckService.class);

    private final Telemetry telemetry = mock(Telemetry.class);

    public BaseRuntimeFixture(Monitor monitor) {
        this(monitor, new ArrayList<>());
    }

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
    protected @NotNull Telemetry createTelemetry() {
        return telemetry;
    }

    @Override
    protected void exit() {
        // do nothing
    }

    @Override
    protected List<InjectionContainer<ServiceExtension>> createExtensions() {

        if (extensions != null && !extensions.isEmpty()) {
            return new DependencyGraph().of(extensions);
        } else {
            return super.createExtensions();
        }
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        var ctx = super.createContext(typeManager, monitor, telemetry);
        ctx.registerService(HealthCheckService.class, healthCheckService);
        return ctx;
    }

    @Override
    protected @NotNull Monitor createMonitor() {
        return monitor;
    }
}
