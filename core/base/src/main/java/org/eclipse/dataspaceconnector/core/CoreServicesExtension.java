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

import net.jodah.failsafe.RetryPolicy;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.core.base.CommandHandlerRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.agent.ParticipantAgentServiceImpl;
import org.eclipse.dataspaceconnector.core.base.policy.PolicyEngineImpl;
import org.eclipse.dataspaceconnector.core.base.policy.RuleBindingRegistryImpl;
import org.eclipse.dataspaceconnector.core.base.policy.ScopeFilter;
import org.eclipse.dataspaceconnector.core.health.HealthCheckServiceConfiguration;
import org.eclipse.dataspaceconnector.core.health.HealthCheckServiceImpl;
import org.eclipse.dataspaceconnector.core.security.DefaultPrivateKeyParseFunction;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.BaseExtension;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentationImplementation;
import org.eclipse.dataspaceconnector.spi.system.Hostname;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;


@BaseExtension
@Provides({
        HealthCheckService.class,
        Hostname.class,
        OkHttpClient.class,
        ParticipantAgentService.class,
        PolicyEngine.class,
        RemoteMessageDispatcherRegistry.class,
        RetryPolicy.class,
        RuleBindingRegistry.class,
        ExecutorInstrumentation.class,
})
public class CoreServicesExtension implements ServiceExtension {

    @EdcSetting
    public static final String MAX_RETRIES = "edc.core.retry.retries.max";
    @EdcSetting
    public static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @EdcSetting
    public static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";
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
     * An optional OkHttp {@link EventListener} that can be used to instrument OkHttp client for collecting metrics.
     * Used by the optional {@code micrometer} module.
     */
    @Inject(required = false)
    private EventListener okHttpEventListener;
    /**
     * An optional instrumentor for {@link ExecutorService}. Used by the optional {@code micrometer} module.
     */
    @Inject(required = false)
    private ExecutorInstrumentationImplementation executorInstrumentationImplementation;

    private HealthCheckServiceImpl healthCheckService;

    @Override
    public String name() {
        return "Core Services";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        addHttpClient(context);
        addRetryPolicy(context);
        registerParser(context);
        var executorInstrumentation = registerExecutorInstrumentation(context);
        var config = getHealthCheckConfig(context);

        // health check service
        healthCheckService = new HealthCheckServiceImpl(config, executorInstrumentation);
        context.registerService(HealthCheckService.class, healthCheckService);

        // remote message dispatcher registry
        var dispatcherRegistry = new RemoteMessageDispatcherRegistryImpl();
        context.registerService(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);

        context.registerService(CommandHandlerRegistry.class, new CommandHandlerRegistryImpl());

        var agentService = new ParticipantAgentServiceImpl();
        context.registerService(ParticipantAgentService.class, agentService);

        var bindingRegistry = new RuleBindingRegistryImpl();
        context.registerService(RuleBindingRegistry.class, bindingRegistry);

        var scopeFilter = new ScopeFilter(bindingRegistry);

        var typeManager = context.getTypeManager();
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);

        var policyEngine = new PolicyEngineImpl(scopeFilter);
        context.registerService(PolicyEngine.class, policyEngine);

        registerHostname(context);
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

    private HealthCheckServiceConfiguration getHealthCheckConfig(ServiceExtensionContext context) {

        return HealthCheckServiceConfiguration.Builder.newInstance()
                .livenessPeriod(Duration.ofSeconds(context.getSetting(LIVENESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .startupStatusPeriod(Duration.ofSeconds(context.getSetting(STARTUP_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .readinessPeriod(Duration.ofSeconds(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .readinessPeriod(Duration.ofSeconds(context.getSetting(READINESS_PERIOD_SECONDS_SETTING, DEFAULT_DURATION)))
                .threadPoolSize(context.getSetting(THREADPOOL_SIZE_SETTING, DEFAULT_TP_SIZE))
                .build();
    }

    private void registerHostname(ServiceExtensionContext context) {
        var hostname = context.getSetting(HOSTNAME_SETTING, DEFAULT_HOSTNAME);
        if (DEFAULT_HOSTNAME.equals(hostname)) {
            context.getMonitor().warning(String.format("Settings: No setting found for key '%s'. Using default value '%s'", HOSTNAME_SETTING, DEFAULT_HOSTNAME));
        }
        context.registerService(Hostname.class, () -> hostname);
    }

    private void registerParser(ServiceExtensionContext context) {
        var resolver = context.getService(PrivateKeyResolver.class);
        resolver.addParser(PrivateKey.class, new DefaultPrivateKeyParseFunction());
    }

    private void addRetryPolicy(ServiceExtensionContext context) {

        var maxRetries = context.getSetting(MAX_RETRIES, 5);
        var minBackoff = context.getSetting(BACKOFF_MIN_MILLIS, 500);
        var maxBackoff = context.getSetting(BACKOFF_MAX_MILLIS, 10_000);

        var retryPolicy = new RetryPolicy<>()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS);

        context.registerService(RetryPolicy.class, retryPolicy);

    }

    private void addHttpClient(ServiceExtensionContext context) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);

        ofNullable(okHttpEventListener).ifPresent(builder::eventListener);

        var client = builder.build();

        context.registerService(OkHttpClient.class, client);
    }

    private ExecutorInstrumentation registerExecutorInstrumentation(ServiceExtensionContext context) {
        var executorInstrumentation = ofNullable((ExecutorInstrumentation) this.executorInstrumentationImplementation)
                .orElse(ExecutorInstrumentation.noop());
        // Register ExecutorImplementation with default noop implementation if none available
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        return executorInstrumentation;
    }
}
