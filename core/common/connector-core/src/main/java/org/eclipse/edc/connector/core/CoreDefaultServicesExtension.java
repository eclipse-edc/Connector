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

package org.eclipse.edc.connector.core;

import dev.failsafe.RetryPolicy;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.core.base.EdcHttpClientImpl;
import org.eclipse.edc.connector.core.base.OkHttpClientConfiguration;
import org.eclipse.edc.connector.core.base.OkHttpClientFactory;
import org.eclipse.edc.connector.core.base.RetryPolicyConfiguration;
import org.eclipse.edc.connector.core.base.RetryPolicyFactory;
import org.eclipse.edc.connector.core.base.agent.NoOpParticipantIdMapper;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.agent.ParticipantIdMapper;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link CoreServicesExtension}
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Default Services";

    private static final String RETRY_POLICY_DEFAULT_RETRIES = "5";
    private static final String RETRY_POLICY_DEFAULT_MIN_BACKOFF = "500";
    private static final String RETRY_POLICY_DEFAULT_MAX_BACKOFF = "10000";
    private static final String RETRY_POLICY_DEFAULT_LOG = "false";
    @Setting(value = "RetryPolicy: Maximum retries before a failure is propagated", defaultValue = RETRY_POLICY_DEFAULT_RETRIES)
    private static final String RETRY_POLICY_MAX_RETRIES = "edc.core.retry.retries.max";
    @Setting(value = "RetryPolicy: Minimum number of milliseconds for exponential backoff", defaultValue = RETRY_POLICY_DEFAULT_MIN_BACKOFF)
    private static final String RETRY_POLICY_BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @Setting(value = "RetryPolicy: Maximum number of milliseconds for exponential backoff.", defaultValue = RETRY_POLICY_DEFAULT_MAX_BACKOFF)
    private static final String RETRY_POLICY_BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";
    @Setting(value = "RetryPolicy: Log onRetry events", defaultValue = RETRY_POLICY_DEFAULT_LOG)
    static final String RETRY_POLICY_LOG_ON_RETRY = "edc.core.retry.log.on.retry";
    @Setting(value = "RetryPolicy: Log onRetryScheduled events", defaultValue = RETRY_POLICY_DEFAULT_LOG)
    static final String RETRY_POLICY_LOG_ON_RETRY_SCHEDULED = "edc.core.retry.log.on.retry.scheduled";
    @Setting(value = "RetryPolicy: Log onRetriesExceeded events", defaultValue = RETRY_POLICY_DEFAULT_LOG)
    static final String RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED = "edc.core.retry.log.on.retries.exceeded";
    @Setting(value = "RetryPolicy: Log onFailedAttempt events", defaultValue = RETRY_POLICY_DEFAULT_LOG)
    static final String RETRY_POLICY_LOG_ON_FAILED_ATTEMPT = "edc.core.retry.log.on.failed.attempt";
    @Setting(value = "RetryPolicy: Log onAbort events", defaultValue = RETRY_POLICY_DEFAULT_LOG)
    static final String RETRY_POLICY_LOG_ON_ABORT = "edc.core.retry.log.on.abort";

    private static final String OK_HTTP_CLIENT_DEFAULT_TIMEOUT = "30";
    private static final String OK_HTTP_CLIENT_DEFAULT_HTTPS_ENFORCE = "false";
    @Setting(value = "OkHttpClient: If true, enable HTTPS call enforcement.", defaultValue = OK_HTTP_CLIENT_DEFAULT_HTTPS_ENFORCE, type = "boolean")
    public static final String OK_HTTP_CLIENT_HTTPS_ENFORCE = "edc.http.client.https.enforce";
    @Setting(value = "OkHttpClient: connect timeout, in seconds", defaultValue = OK_HTTP_CLIENT_DEFAULT_TIMEOUT, type = "int")
    public static final String OK_HTTP_CLIENT_TIMEOUT_CONNECT = "edc.http.client.timeout.connect";
    @Setting(value = "OkHttpClient: read timeout, in seconds", defaultValue = OK_HTTP_CLIENT_DEFAULT_TIMEOUT, type = "int")
    public static final String OK_HTTP_CLIENT_TIMEOUT_READ = "edc.http.client.timeout.read";
    @Setting(value = "OkHttpClient: send buffer size, in bytes", type = "int", min = 1)
    public static final String OK_HTTP_CLIENT_SEND_BUFFER_SIZE = "edc.http.client.send.buffer.size";
    @Setting(value = "OkHttpClient: receive buffer size, in bytes", type = "int", min = 1)
    public static final String OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE = "edc.http.client.receive.buffer.size";

    /**
     * An optional OkHttp {@link EventListener} that can be used to instrument OkHttp client for collecting metrics.
     */
    @Inject(required = false)
    private EventListener okHttpEventListener;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public TransactionContext defaultTransactionContext(ServiceExtensionContext context) {
        context.getMonitor().warning("No TransactionContext registered, a no-op implementation will be used, not suitable for production environments");
        return new NoopTransactionContext();
    }

    @Provider(isDefault = true)
    public DataSourceRegistry dataSourceRegistry(ServiceExtensionContext context) {
        context.getMonitor().warning("No DataSourceRegistry registered, DefaultDataSourceRegistry will be used, not suitable for production environments");
        return new DefaultDataSourceRegistry();
    }

    @Provider(isDefault = true)
    public ExecutorInstrumentation defaultInstrumentation() {
        return ExecutorInstrumentation.noop();
    }

    @Provider(isDefault = true)
    public EventExecutorServiceContainer eventExecutorServiceContainer() {
        return new EventExecutorServiceContainer(Executors.newFixedThreadPool(1));
    }

    @Provider(isDefault = true)
    public Vault vault(ServiceExtensionContext context) {
        return createInmemVault(context);
    }


    @Provider
    public EdcHttpClient edcHttpClient(ServiceExtensionContext context) {
        return new EdcHttpClientImpl(
                okHttpClient(context),
                retryPolicy(context),
                context.getMonitor()
        );
    }

    @Provider
    public OkHttpClient okHttpClient(ServiceExtensionContext context) {
        var configuration = OkHttpClientConfiguration.Builder.newInstance()
                .enforceHttps(context.getSetting(OK_HTTP_CLIENT_HTTPS_ENFORCE, Boolean.parseBoolean(OK_HTTP_CLIENT_DEFAULT_HTTPS_ENFORCE)))
                .connectTimeout(context.getSetting(OK_HTTP_CLIENT_TIMEOUT_CONNECT, parseInt(OK_HTTP_CLIENT_DEFAULT_TIMEOUT)))
                .readTimeout(context.getSetting(OK_HTTP_CLIENT_TIMEOUT_READ, parseInt(OK_HTTP_CLIENT_DEFAULT_TIMEOUT)))
                .sendBufferSize(context.getSetting(OK_HTTP_CLIENT_SEND_BUFFER_SIZE, 0))
                .receiveBufferSize(context.getSetting(OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE, 0))
                .build();

        return OkHttpClientFactory.create(configuration, okHttpEventListener, context.getMonitor());
    }

    @Provider
    public <T> RetryPolicy<T> retryPolicy(ServiceExtensionContext context) {
        var configuration = RetryPolicyConfiguration.Builder.newInstance()
                .maxRetries(context.getSetting(RETRY_POLICY_MAX_RETRIES, parseInt(RETRY_POLICY_DEFAULT_RETRIES)))
                .minBackoff(context.getSetting(RETRY_POLICY_BACKOFF_MIN_MILLIS, parseInt(RETRY_POLICY_DEFAULT_MIN_BACKOFF)))
                .maxBackoff(context.getSetting(RETRY_POLICY_BACKOFF_MAX_MILLIS, parseInt(RETRY_POLICY_DEFAULT_MAX_BACKOFF)))
                .logOnRetry(context.getSetting(RETRY_POLICY_LOG_ON_RETRY, false))
                .logOnRetryScheduled(context.getSetting(RETRY_POLICY_LOG_ON_RETRY_SCHEDULED, false))
                .logOnRetriesExceeded(context.getSetting(RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED, false))
                .logOnFailedAttempt(context.getSetting(RETRY_POLICY_LOG_ON_FAILED_ATTEMPT, false))
                .logOnAbort(context.getSetting(RETRY_POLICY_LOG_ON_ABORT, false))
                .build();

        return RetryPolicyFactory.create(configuration, context.getMonitor());
    }

    @Provider(isDefault = true)
    public Vault createInmemVault(ServiceExtensionContext context) {
        context.getMonitor().warning("Using the InMemoryVault is not suitable for production scenarios and should be replaced with an actual Vault!");
        return new InMemoryVault(context.getMonitor());
    }

    @Provider(isDefault = true)
    public ParticipantIdMapper participantIdMapper() {
        return new NoOpParticipantIdMapper();
    }

}
