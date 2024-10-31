/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.boot;

import org.eclipse.edc.boot.apiversion.ApiVersionServiceImpl;
import org.eclipse.edc.boot.health.HealthCheckServiceImpl;
import org.eclipse.edc.boot.system.ExtensionLoader;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;


@Extension(value = BootServicesExtension.NAME)
public class BootServicesExtension implements ServiceExtension {

    public static final String NAME = "Boot Services";

    @Setting(value = "Configures the participant id this runtime is operating on behalf of")
    public static final String PARTICIPANT_ID = "edc.participant.id";

    @Setting(value = "Configures the runtime id. This should be fully or partly randomized, and need not be stable across restarts. It is recommended to leave this value blank.", defaultValue = "<random UUID>")
    public static final String RUNTIME_ID = "edc.runtime.id";

    @Setting(value = "Configures this component's ID. This should be a unique, stable and deterministic identifier.", defaultValue = "<random UUID>")
    public static final String COMPONENT_ID = "edc.component.id";

    private HealthCheckServiceImpl healthCheckService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        healthCheckService = new HealthCheckServiceImpl();
    }


    @Provider
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Provider
    public Telemetry telemetry() {
        return ExtensionLoader.loadTelemetry();
    }

    @Provider
    public HealthCheckService healthCheckService() {
        return healthCheckService;
    }

    @Provider(isDefault = true)
    public Vault vault(ServiceExtensionContext context) {
        return createInmemVault(context);
    }

    @Provider(isDefault = true)
    public ExecutorInstrumentation defaultInstrumentation() {
        return ExecutorInstrumentation.noop();
    }

    @Provider(isDefault = true)
    public Vault createInmemVault(ServiceExtensionContext context) {
        context.getMonitor().warning("Using the InMemoryVault is not suitable for production scenarios and should be replaced with an actual Vault!");
        return new InMemoryVault(context.getMonitor());
    }

    @Provider
    public ApiVersionService apiVersionService() {
        return new ApiVersionServiceImpl();
    }


}
