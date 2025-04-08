/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.runtime.core;

import dev.failsafe.RetryPolicy;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.eclipse.edc.runtime.core.http.OkHttpClientConfiguration;
import org.eclipse.edc.runtime.core.http.OkHttpClientFactory;
import org.eclipse.edc.runtime.core.retry.RetryPolicyConfiguration;
import org.eclipse.edc.runtime.core.retry.RetryPolicyFactory;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.runtime.core.RuntimeDefaultCoreServicesExtension.NAME;

@Extension(NAME)
public class RuntimeDefaultCoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Runtime Default Core Services";

    @Configuration
    private RetryPolicyConfiguration retryPolicyConfiguration;
    @Configuration
    private OkHttpClientConfiguration configuration;

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

    @Provider
    public <T> RetryPolicy<T> retryPolicy(ServiceExtensionContext context) {
        return RetryPolicyFactory.create(retryPolicyConfiguration, context.getMonitor());
    }

    @Provider
    public OkHttpClient okHttpClient(ServiceExtensionContext context) {
        return OkHttpClientFactory.create(configuration, okHttpEventListener, context.getMonitor());
    }

}
