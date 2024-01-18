/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_API_HEALTH_PATH;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_API_HEALTH_PATH_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_API_SECRET_PATH;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_API_SECRET_PATH_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_HEALTH_CHECK_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_HEALTH_CHECK_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_HEALTH_CHECK_STANDBY_OK;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;

@Extension(value = HashicorpVaultExtension.NAME)
public class HashicorpVaultExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Vault Extension";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private ScheduledExecutorService scheduledExecutorService;
    private HashicorpVaultClient hashicorpVaultClient;
    private Monitor monitor;
    private boolean isScheduledTokenRenewalEnabled;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public HashicorpVaultClient hashicorpVaultClient(ServiceExtensionContext context) {
        hashicorpVaultClient = hashicorpVaultClient == null ? createHashicorpVaultClient(context, monitor) : hashicorpVaultClient;
        return hashicorpVaultClient;
    }

    @Provider
    public Vault hashicorpVault(ServiceExtensionContext context) {
        return new HashicorpVault(hashicorpVaultClient(context), monitor);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix(NAME);
        scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), NAME);
        isScheduledTokenRenewalEnabled = context.getSetting(VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED, VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED_DEFAULT);
    }

    @Override
    public void start() {
        if (isScheduledTokenRenewalEnabled) {
            hashicorpVaultClient.scheduleTokenRenewal();
        }
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdownNow();
    }

    private HashicorpVaultClient createHashicorpVaultClient(ServiceExtensionContext context, Monitor monitor) {
        var url = context.getSetting(VAULT_URL, null);
        var healthCheckEnabled = context.getSetting(VAULT_HEALTH_CHECK_ENABLED, VAULT_HEALTH_CHECK_ENABLED_DEFAULT);
        var healthCheckPath = context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);
        var healthStandbyOk = context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
        var retryBackoffBase = context.getSetting(VAULT_RETRY_BACKOFF_BASE, VAULT_RETRY_BACKOFF_BASE_DEFAULT);
        var token = context.getSetting(VAULT_TOKEN, null);
        var ttl = context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_DEFAULT);
        var renewBuffer = context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        var secretPath = context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);

        var configValues = HashicorpVaultConfigValues.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(healthCheckEnabled)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(healthStandbyOk)
                .retryBackoffBase(retryBackoffBase)
                .token(token)
                .ttl(ttl)
                .renewBuffer(renewBuffer)
                .secretPath(secretPath)
                .build();

        return new HashicorpVaultClient(
                httpClient,
                typeManager.getMapper(),
                scheduledExecutorService,
                monitor,
                configValues);
    }
}
