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
import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.core.agent.NoOpParticipantIdMapper;
import org.eclipse.edc.connector.core.base.OkHttpClientConfiguration;
import org.eclipse.edc.connector.core.base.OkHttpClientFactory;
import org.eclipse.edc.connector.core.base.RetryPolicyConfiguration;
import org.eclipse.edc.connector.core.base.RetryPolicyFactory;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link CoreServicesExtension}
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Default Services";

    private static final int DEFAULT_RETRY_POLICY_MAX_RETRIES = 5;
    private static final int DEFAULT_RETRY_POLICY_BACKOFF_MIN_MILLIS = 500;
    private static final int DEFAULT_RETRY_POLICY_BACKOFF_MAX_MILLIS = 10000;
    private static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRY = false;
    private static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRY_SCHEDULED = false;
    private static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED = false;
    private static final boolean DEFAULT_RETRY_POLICY_LOG_ON_FAILED_ATTEMPT = false;
    private static final boolean DEFAULT_RETRY_POLICY_LOG_ON_ABORT = false;
    private static final int DEFAULT_OK_HTTP_CLIENT_TIMEOUT_CONNECT = 30;
    private static final int DEFAULT_OK_HTTP_CLIENT_TIMEOUT_READ = 30;
    private static final boolean DEFAULT_OK_HTTP_CLIENT_HTTPS_ENFORCE = false;
    private static final int DEFAULT_OK_HTTP_CLIENT_SEND_BUFFER_SIZE = 0;
    private static final int DEFAULT_OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE = 0;

    @Setting(value = "RetryPolicy: Maximum retries before a failure is propagated", defaultValue = DEFAULT_RETRY_POLICY_MAX_RETRIES + "", type = "int")
    private static final String RETRY_POLICY_MAX_RETRIES = "edc.core.retry.retries.max";
    @Setting(value = "RetryPolicy: Minimum number of milliseconds for exponential backoff", defaultValue = DEFAULT_RETRY_POLICY_BACKOFF_MIN_MILLIS + "", type = "int")
    private static final String RETRY_POLICY_BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @Setting(value = "RetryPolicy: Maximum number of milliseconds for exponential backoff", defaultValue = DEFAULT_RETRY_POLICY_BACKOFF_MAX_MILLIS + "", type = "int")
    private static final String RETRY_POLICY_BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";
    @Setting(value = "RetryPolicy: Log onRetry events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRY + "", type = "boolean")
    private static final String RETRY_POLICY_LOG_ON_RETRY = "edc.core.retry.log.on.retry";
    @Setting(value = "RetryPolicy: Log onRetryScheduled events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRY_SCHEDULED + "", type = "boolean")
    private static final String RETRY_POLICY_LOG_ON_RETRY_SCHEDULED = "edc.core.retry.log.on.retry.scheduled";
    @Setting(value = "RetryPolicy: Log onRetriesExceeded events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED + "", type = "boolean")
    private static final String RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED = "edc.core.retry.log.on.retries.exceeded";
    @Setting(value = "RetryPolicy: Log onFailedAttempt events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_FAILED_ATTEMPT + "", type = "boolean")
    private static final String RETRY_POLICY_LOG_ON_FAILED_ATTEMPT = "edc.core.retry.log.on.failed.attempt";
    @Setting(value = "RetryPolicy: Log onAbort events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_ABORT + "", type = "boolean")
    private static final String RETRY_POLICY_LOG_ON_ABORT = "edc.core.retry.log.on.abort";
    @Setting(value = "OkHttpClient: If true, enable HTTPS call enforcement", defaultValue = DEFAULT_OK_HTTP_CLIENT_HTTPS_ENFORCE + "", type = "boolean")
    private static final String OK_HTTP_CLIENT_HTTPS_ENFORCE = "edc.http.client.https.enforce";
    @Setting(value = "OkHttpClient: connect timeout, in seconds", defaultValue = DEFAULT_OK_HTTP_CLIENT_TIMEOUT_CONNECT + "", type = "int")
    private static final String OK_HTTP_CLIENT_TIMEOUT_CONNECT = "edc.http.client.timeout.connect";
    @Setting(value = "OkHttpClient: read timeout, in seconds", defaultValue = DEFAULT_OK_HTTP_CLIENT_TIMEOUT_READ + "", type = "int")
    private static final String OK_HTTP_CLIENT_TIMEOUT_READ = "edc.http.client.timeout.read";
    @Setting(value = "OkHttpClient: send buffer size, in bytes", defaultValue = DEFAULT_OK_HTTP_CLIENT_SEND_BUFFER_SIZE + "", type = "int", min = 1)
    private static final String OK_HTTP_CLIENT_SEND_BUFFER_SIZE = "edc.http.client.send.buffer.size";
    @Setting(value = "OkHttpClient: receive buffer size, in bytes", defaultValue = DEFAULT_OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE + "", type = "int", min = 1)
    private static final String OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE = "edc.http.client.receive.buffer.size";

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
    public EventExecutorServiceContainer eventExecutorServiceContainer() {
        return new EventExecutorServiceContainer(Executors.newFixedThreadPool(1));
    }

    @Provider
    public EdcHttpClient edcHttpClient(ServiceExtensionContext context) {
        return new EdcHttpClientImpl(
                okHttpClient(context),
                retryPolicy(context),
                context.getMonitor()
        );
    }

    @Provider(isDefault = true)
    public ControlClientAuthenticationProvider controlClientAuthenticationProvider() {
        return Collections::emptyMap;
    }

    @Provider
    public OkHttpClient okHttpClient(ServiceExtensionContext context) {
        var configuration = OkHttpClientConfiguration.Builder.newInstance()
                .enforceHttps(context.getSetting(OK_HTTP_CLIENT_HTTPS_ENFORCE, DEFAULT_OK_HTTP_CLIENT_HTTPS_ENFORCE))
                .connectTimeout(context.getSetting(OK_HTTP_CLIENT_TIMEOUT_CONNECT, DEFAULT_OK_HTTP_CLIENT_TIMEOUT_CONNECT))
                .readTimeout(context.getSetting(OK_HTTP_CLIENT_TIMEOUT_READ, DEFAULT_OK_HTTP_CLIENT_TIMEOUT_READ))
                .sendBufferSize(context.getSetting(OK_HTTP_CLIENT_SEND_BUFFER_SIZE, DEFAULT_OK_HTTP_CLIENT_SEND_BUFFER_SIZE))
                .receiveBufferSize(context.getSetting(OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE, DEFAULT_OK_HTTP_CLIENT_RECEIVE_BUFFER_SIZE))
                .build();

        return OkHttpClientFactory.create(configuration, okHttpEventListener, context.getMonitor());
    }

    @Provider
    public <T> RetryPolicy<T> retryPolicy(ServiceExtensionContext context) {
        var configuration = RetryPolicyConfiguration.Builder.newInstance()
                .maxRetries(context.getSetting(RETRY_POLICY_MAX_RETRIES, DEFAULT_RETRY_POLICY_MAX_RETRIES))
                .minBackoff(context.getSetting(RETRY_POLICY_BACKOFF_MIN_MILLIS, DEFAULT_RETRY_POLICY_BACKOFF_MIN_MILLIS))
                .maxBackoff(context.getSetting(RETRY_POLICY_BACKOFF_MAX_MILLIS, DEFAULT_RETRY_POLICY_BACKOFF_MAX_MILLIS))
                .logOnRetry(context.getSetting(RETRY_POLICY_LOG_ON_RETRY, DEFAULT_RETRY_POLICY_LOG_ON_RETRY))
                .logOnRetryScheduled(context.getSetting(RETRY_POLICY_LOG_ON_RETRY_SCHEDULED, DEFAULT_RETRY_POLICY_LOG_ON_RETRY_SCHEDULED))
                .logOnRetriesExceeded(context.getSetting(RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED, DEFAULT_RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED))
                .logOnFailedAttempt(context.getSetting(RETRY_POLICY_LOG_ON_FAILED_ATTEMPT, DEFAULT_RETRY_POLICY_LOG_ON_FAILED_ATTEMPT))
                .logOnAbort(context.getSetting(RETRY_POLICY_LOG_ON_ABORT, DEFAULT_RETRY_POLICY_LOG_ON_ABORT))
                .build();

        return RetryPolicyFactory.create(configuration, context.getMonitor());
    }

    @Provider(isDefault = true)
    public ParticipantIdMapper participantIdMapper() {
        return new NoOpParticipantIdMapper();
    }

}
