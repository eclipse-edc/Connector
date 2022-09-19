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

package org.eclipse.dataspaceconnector.core;

import dev.failsafe.RetryPolicy;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.core.base.OkHttpClientFactory;
import org.eclipse.dataspaceconnector.core.event.EventExecutorServiceContainer;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopCertificateResolver;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.vault.NoopVault;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

/**
 * Provides default service implementations for fallback
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    @EdcSetting(value = "Maximum retries for the retry policy before a failure is propagated")
    public static final String MAX_RETRIES = "edc.core.retry.retries.max";
    @EdcSetting(value = "Minimum number of milliseconds for exponential backoff")
    public static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @EdcSetting(value = "Maximum number of milliseconds for exponential backoff. ")
    public static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";

    /**
     * An optional OkHttp {@link EventListener} that can be used to instrument OkHttp client for collecting metrics.
     */
    @Inject(required = false)
    private EventListener okHttpEventListener;

    @Override
    public String name() {
        return "Core Default Services";
    }

    @Provider(isDefault = true)
    public TransactionContext defaultTransactionContext(ServiceExtensionContext context) {
        context.getMonitor().warning("No TransactionContext registered, a no-op implementation will be used, not suitable for production environments");
        return new NoopTransactionContext();
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
