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
import org.eclipse.edc.connector.core.base.OkHttpClientFactory;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.vault.NoopCertificateResolver;
import org.eclipse.edc.spi.system.vault.NoopPrivateKeyResolver;
import org.eclipse.edc.spi.system.vault.NoopVault;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link CoreServicesExtension}
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    @Setting(value = "Maximum retries for the retry policy before a failure is propagated")
    public static final String MAX_RETRIES = "edc.core.retry.retries.max";
    @Setting(value = "Minimum number of milliseconds for exponential backoff")
    public static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @Setting(value = "Maximum number of milliseconds for exponential backoff. ")
    public static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";
    public static final String NAME = "Core Default Services";

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

    @Provider
    public OkHttpClient okHttpClient(ServiceExtensionContext context) {
        return OkHttpClientFactory.create(context, okHttpEventListener);
    }

    @Provider
    public RetryPolicy<?> retryPolicy(ServiceExtensionContext context) {
        var maxRetries = context.getSetting(MAX_RETRIES, 5);
        var minBackoff = context.getSetting(BACKOFF_MIN_MILLIS, 500);
        var maxBackoff = context.getSetting(BACKOFF_MAX_MILLIS, 10_000);

        return RetryPolicy.builder()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS)
                .build();
    }

}
