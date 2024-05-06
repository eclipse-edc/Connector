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

import org.eclipse.edc.boot.health.HealthCheckServiceConfiguration;
import org.eclipse.edc.boot.health.HealthCheckServiceImpl;
import org.eclipse.edc.boot.system.ExtensionLoader;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;
import java.time.Duration;


@BaseExtension
@Extension(value = BootServicesExtension.NAME)
public class BootServicesExtension implements ServiceExtension {

    public static final String NAME = "Boot Services";

    @Setting
    public static final String LIVENESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.liveness-period";
    @Setting
    public static final String STARTUP_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.startup-period";
    @Setting
    public static final String READINESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.readiness-period";
    @Setting
    public static final String THREADPOOL_SIZE_SETTING = "edc.core.system.health.check.threadpool-size";
    @Setting(value = "Configures the participant id this runtime is operating on behalf of")
    public static final String PARTICIPANT_ID = "edc.participant.id";

    @Setting(value = "Configures the runtime id", defaultValue = "<random UUID>")
    public static final String RUNTIME_ID = "edc.runtime.id";

    private static final long DEFAULT_DURATION = 60;
    private static final int DEFAULT_TP_SIZE = 3;

    @Inject
    private ExecutorInstrumentation instrumentation;

    private HealthCheckServiceImpl healthCheckService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = getHealthCheckConfig(context);
        healthCheckService = new HealthCheckServiceImpl(config, instrumentation);
    }

    @Override
    public void start() {
        healthCheckService.start();
    }

    @Override
    public void shutdown() {
        healthCheckService.stop();
        ServiceExtension.super.shutdown();
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

    private HealthCheckServiceConfiguration getHealthCheckConfig(ServiceExtensionContext context) {
        return HealthCheckServiceConfiguration.Builder.newInstance()
                .livenessPeriod(Duration.ofSeconds(context.getSetting(LIVENESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .startupStatusPeriod(Duration.ofSeconds(context.getSetting(STARTUP_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .readinessPeriod(Duration.ofSeconds(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .readinessPeriod(Duration.ofSeconds(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .threadPoolSize(context.getSetting(THREADPOOL_SIZE_SETTING, DEFAULT_TP_SIZE))
                .build();
    }


}
