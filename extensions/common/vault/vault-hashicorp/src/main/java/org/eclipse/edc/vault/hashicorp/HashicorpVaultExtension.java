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
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Duration;
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
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TIMEOUT_SECONDS;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TIMEOUT_SECONDS_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL_SECONDS_DEFAULT;
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

    private Vault vault;
    private PrivateKeyResolver privateKeyResolver;
    private ScheduledExecutorService scheduledExecutorService;
    private HashicorpVaultClient hashicorpVaultClient;
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault vault() {
        return vault;
    }

    @Provider
    public PrivateKeyResolver privateKeyResolver() {
        return privateKeyResolver;
    }

    @Provider
    public HashicorpVaultClient hashicorpVaultClient() {
        return hashicorpVaultClient;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), NAME);
        monitor = context.getMonitor().withPrefix(NAME);
        hashicorpVaultClient = createHashicorpVaultClient(context, monitor);
        vault = new HashicorpVault(hashicorpVaultClient, monitor);
        privateKeyResolver = new VaultPrivateKeyResolver(vault);
        context.registerService(CertificateResolver.class, new HashicorpCertificateResolver(vault, monitor));
    }

    @Override
    public void start() {
        var tokenLookUpResult = hashicorpVaultClient.lookUpToken();

        if (tokenLookUpResult.failed()) {
            throw new EdcException("[%s] Initial token look up failed: %s".formatted(NAME, tokenLookUpResult.getFailureDetail()));
        }

        var tokenLookUp = tokenLookUpResult.getContent();

        if (tokenLookUp.isRootToken()) {
            monitor.warning("Root token detected. Do not use root tokens in production environment.");
        }

        if (tokenLookUp.isRenewable()) {
            var tokenRenewResult = hashicorpVaultClient.renewToken();

            if (tokenRenewResult.failed()) {
                throw new EdcException("[%s] Initial token renewal failed: %s".formatted(NAME, tokenRenewResult.getFailureDetail()));
            }

            var tokenRenew = tokenRenewResult.getContent();
            hashicorpVaultClient.scheduleNextTokenRenewal(tokenRenew.getTimeToLive());
        }
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdownNow();
    }

    private HashicorpVaultClient createHashicorpVaultClient(ServiceExtensionContext context, Monitor monitor) {
        var configValues = getConfigValues(context);

        return new HashicorpVaultClient(
                httpClient,
                typeManager.getMapper(),
                scheduledExecutorService,
                monitor,
                configValues);
    }

    private HashicorpVaultConfigValues getConfigValues(ServiceExtensionContext context) {
        var url = context.getSetting(VAULT_URL, null);
        if (url == null) {
            throw new EdcException("[%s] Vault URL must not be null");
        }

        var healthCheckEnabled = context.getSetting(VAULT_HEALTH_CHECK_ENABLED, VAULT_HEALTH_CHECK_ENABLED_DEFAULT);
        var healthCheckPath = context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);
        var healthStandbyOk = context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
        var timeoutSeconds = Math.max(0, context.getSetting(VAULT_TIMEOUT_SECONDS, VAULT_TIMEOUT_SECONDS_DEFAULT));
        var timeoutDuration = Duration.ofSeconds(timeoutSeconds);
        var token = context.getSetting(VAULT_TOKEN, null);
        if (null == token) {
            throw new EdcException("[%s] Vault token must not be null");
        }
        var timeToLive = context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_SECONDS_DEFAULT);
        var renewBuffer = context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        var secretPath = context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);

        return HashicorpVaultConfigValues.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(healthCheckEnabled)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(healthStandbyOk)
                .timeoutDuration(timeoutDuration)
                .token(token)
                .timeToLive(timeToLive)
                .renewBuffer(renewBuffer)
                .secretPath(secretPath)
                .build();
    }
}
