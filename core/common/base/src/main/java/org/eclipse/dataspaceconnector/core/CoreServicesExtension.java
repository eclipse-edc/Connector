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

package org.eclipse.dataspaceconnector.core;

import org.eclipse.dataspaceconnector.core.base.CommandHandlerRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.agent.ParticipantAgentServiceImpl;
import org.eclipse.dataspaceconnector.core.base.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.base.policy.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.policy.ScopeFilter;
import org.eclipse.dataspaceconnector.core.event.EventExecutorServiceContainer;
import org.eclipse.dataspaceconnector.core.event.EventRouterImpl;
import org.eclipse.dataspaceconnector.core.health.HealthCheckServiceConfiguration;
import org.eclipse.dataspaceconnector.core.health.HealthCheckServiceImpl;
import org.eclipse.dataspaceconnector.core.security.DefaultPrivateKeyParseFunction;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.engine.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.BaseExtension;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Hostname;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopCertificateResolver;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopVault;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@BaseExtension
@Provides({
        HealthCheckService.class,
        Monitor.class,
        TypeManager.class,
        Clock.class,
        Telemetry.class
})
public class CoreServicesExtension implements ServiceExtension {

    @EdcSetting
    public static final String LIVENESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.liveness-period";
    @EdcSetting
    public static final String STARTUP_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.startup-period";
    @EdcSetting
    public static final String READINESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.readiness-period";
    @EdcSetting
    public static final String THREADPOOL_SIZE_SETTING = "edc.core.system.health.check.threadpool-size";
    @EdcSetting
    public static final String HOSTNAME_SETTING = "edc.hostname";

    private static final long DEFAULT_DURATION = 60;
    private static final int DEFAULT_TP_SIZE = 3;
    private static final String DEFAULT_HOSTNAME = "localhost";


    /**
     * An optional instrumentor for {@link ExecutorService}. Used by the optional {@code micrometer} module.
     */
    @Inject(required = false)
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject(required = false)
    private EventExecutorServiceContainer eventExecutorServiceContainer;

    private HealthCheckServiceImpl healthCheckService;
    private RuleBindingRegistry ruleBindingRegistry;
    private ScopeFilter scopeFilter;

    @Override
    public String name() {
        return "Core Services";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        privateKeyResolver.addParser(PrivateKey.class, new DefaultPrivateKeyParseFunction());

        var config = getHealthCheckConfig(context);

        // health check service
        healthCheckService = new HealthCheckServiceImpl(config, executorInstrumentation);
        context.registerService(HealthCheckService.class, healthCheckService);

        ruleBindingRegistry = new RuleBindingRegistryImpl();

        scopeFilter = new ScopeFilter(ruleBindingRegistry);

        var typeManager = context.getTypeManager();
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
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

    @Provider(isDefault = true)
    public ExecutorInstrumentation defaultInstrumentation() {
        return ExecutorInstrumentation.noop();
    }

    @Provider
    public Hostname hostname(ServiceExtensionContext context) {
        var hostname = context.getSetting(HOSTNAME_SETTING, DEFAULT_HOSTNAME);
        if (DEFAULT_HOSTNAME.equals(hostname)) {
            context.getMonitor().warning(String.format("Settings: No setting found for key '%s'. Using default value '%s'", HOSTNAME_SETTING, DEFAULT_HOSTNAME));
        }
        return () -> hostname;
    }

    @Provider
    public RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry() {
        return new RemoteMessageDispatcherRegistryImpl();
    }

    @Provider
    public CommandHandlerRegistry commandHandlerRegistry() {
        return new CommandHandlerRegistryImpl();
    }

    @Provider
    public ParticipantAgentService participantAgentService() {
        return new ParticipantAgentServiceImpl();
    }

    @Provider
    public RuleBindingRegistry ruleBindingRegistry() {
        return ruleBindingRegistry;
    }

    @Provider
    public PolicyEngine policyEngine() {
        return new PolicyEngineImpl(scopeFilter);
    }

    @Provider
    public EventRouter eventRouter(ServiceExtensionContext context) {
        if (eventExecutorServiceContainer == null) {
            eventExecutorServiceContainer = eventExecutorServiceContainer();
        }
        return new EventRouterImpl(context.getMonitor(), eventExecutorServiceContainer.getExecutorService());
    }

    @Provider(isDefault = true)
    public EventExecutorServiceContainer eventExecutorServiceContainer() {
        return new EventExecutorServiceContainer(Executors.newFixedThreadPool(1)); // TODO: make configurable
    }

    @Provider(isDefault = true)
    public Vault vault() {
        return new NoopVault();
    }

    @Provider(isDefault = true)
    public PrivateKeyResolver privateKeyResolver() {
        return new NoopPrivateKeyResolver();
    }

    @Provider(isDefault = true)
    public CertificateResolver certificateResolver() {
        return new NoopCertificateResolver();
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

