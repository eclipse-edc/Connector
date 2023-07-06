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
import org.eclipse.edc.connector.core.base.OkHttpClientFactory;
import org.eclipse.edc.connector.core.base.RetryPolicyFactory;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultCertificateResolver;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.concurrent.Executors;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link CoreServicesExtension}
 */
public class CoreDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Default Services";

    /**
     * An optional OkHttp {@link EventListener} that can be used to instrument OkHttp client for collecting metrics.
     */
    @Inject(required = false)
    private EventListener okHttpEventListener;
    private InMemoryVault inMemoryVault;

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
    public Vault vault(ServiceExtensionContext context) {
        return getVault(context);
    }

    @Provider(isDefault = true)
    public PrivateKeyResolver privateKeyResolver(ServiceExtensionContext context) {
        return new VaultPrivateKeyResolver(getVault(context));
    }

    @Provider(isDefault = true)
    public CertificateResolver certificateResolver(ServiceExtensionContext context) {
        return new VaultCertificateResolver(getVault(context));
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
        return OkHttpClientFactory.create(context, okHttpEventListener);
    }

    @Provider
    public <T> RetryPolicy<T> retryPolicy(ServiceExtensionContext context) {
        return RetryPolicyFactory.create(context);
    }

    /**
     * lazily instantiates the default vault impl, which is an im-memory one.
     */
    private Vault getVault(ServiceExtensionContext context) {
        if (inMemoryVault == null) {
            inMemoryVault = new InMemoryVault(context.getMonitor());
        }
        return inMemoryVault;
    }

}
