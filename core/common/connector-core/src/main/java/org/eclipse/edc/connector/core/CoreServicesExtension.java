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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.connector.core.base.CommandHandlerRegistryImpl;
import org.eclipse.edc.connector.core.base.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.edc.connector.core.base.agent.ParticipantAgentServiceImpl;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.core.event.EventRouterImpl;
import org.eclipse.edc.connector.core.health.HealthCheckServiceConfiguration;
import org.eclipse.edc.connector.core.health.HealthCheckServiceImpl;
import org.eclipse.edc.connector.core.security.DefaultPrivateKeyParseFunction;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

import java.security.PrivateKey;
import java.time.Duration;

import static org.eclipse.edc.spi.agent.ParticipantAgentService.DEFAULT_IDENTITY_CLAIM_KEY;

@BaseExtension
@Extension(value = CoreServicesExtension.NAME)
public class CoreServicesExtension implements ServiceExtension {

    @Setting
    public static final String LIVENESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.liveness-period";
    @Setting
    public static final String STARTUP_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.startup-period";
    @Setting
    public static final String READINESS_PERIOD_SECONDS_SETTING = "edc.core.system.health.check.readiness-period";
    @Setting
    public static final String THREADPOOL_SIZE_SETTING = "edc.core.system.health.check.threadpool-size";
    @Setting
    public static final String HOSTNAME_SETTING = "edc.hostname";
    @Setting
    public static final String IDENTITY_KEY = "edc.agent.identity.key";

    public static final String NAME = "Core Services";
    private static final long DEFAULT_DURATION = 60;
    private static final int DEFAULT_TP_SIZE = 3;
    private static final String DEFAULT_HOSTNAME = "localhost";
    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private EventExecutorServiceContainer eventExecutorServiceContainer;

    @Inject
    private TypeManager typeManager;

    private HealthCheckServiceImpl healthCheckService;
    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        privateKeyResolver.addParser(PrivateKey.class, new DefaultPrivateKeyParseFunction());

        var config = getHealthCheckConfig(context);

        healthCheckService = new HealthCheckServiceImpl(config, executorInstrumentation);
        ruleBindingRegistry = new RuleBindingRegistryImpl();

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
    public ParticipantAgentService participantAgentService(ServiceExtensionContext context) {
        var identityKey = context.getSetting(IDENTITY_KEY, DEFAULT_IDENTITY_CLAIM_KEY);
        return new ParticipantAgentServiceImpl(identityKey);
    }

    @Provider
    public RuleBindingRegistry ruleBindingRegistry() {
        return ruleBindingRegistry;
    }

    @Provider
    public PolicyEngine policyEngine() {
        var scopeFilter = new ScopeFilter(ruleBindingRegistry);
        return new PolicyEngineImpl(scopeFilter);
    }

    @Provider
    public EventRouter eventRouter(ServiceExtensionContext context) {
        return new EventRouterImpl(context.getMonitor(), eventExecutorServiceContainer.getExecutorService());
    }

    @Provider
    public HealthCheckService healthCheckService() {
        return healthCheckService;
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

