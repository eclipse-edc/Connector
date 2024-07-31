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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultClient;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultTokenRenewTask;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Extension(value = HashicorpVaultExtension.NAME)
public class HashicorpVaultExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Vault";

    public static final boolean VAULT_HEALTH_CHECK_ENABLED_DEFAULT = true;
    public static final boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;
    public static final String VAULT_API_HEALTH_PATH_DEFAULT = "/v1/sys/health";
    public static final boolean VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT = true;
    public static final long VAULT_TOKEN_RENEW_BUFFER_DEFAULT = 30;
    public static final long VAULT_TOKEN_TTL_DEFAULT = 300;
    public static final String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";

    @Setting(value = "The URL of the Hashicorp Vault", required = true)
    public static final String VAULT_URL = "edc.vault.hashicorp.url";

    @Setting(value = "Whether or not the vault health check is enabled", defaultValue = "true", type = "boolean")
    public static final String VAULT_HEALTH_CHECK_ENABLED = "edc.vault.hashicorp.health.check.enabled";

    @Setting(value = "The URL path of the vault's /health endpoint", defaultValue = VAULT_API_HEALTH_PATH_DEFAULT)
    public static final String VAULT_API_HEALTH_PATH = "edc.vault.hashicorp.api.health.check.path";

    @Setting(value = "Specifies if being a standby should still return the active status code instead of the standby status code", defaultValue = "false", type = "boolean")
    public static final String VAULT_HEALTH_CHECK_STANDBY_OK = "edc.vault.hashicorp.health.check.standby.ok";

    @Setting(value = "The token used to access the Hashicorp Vault", required = true)
    public static final String VAULT_TOKEN = "edc.vault.hashicorp.token";

    @Setting(value = "Whether the automatic token renewal process will be triggered or not. Should be disabled only for development and testing purposes", defaultValue = "true")
    public static final String VAULT_TOKEN_SCHEDULED_RENEW_ENABLED = "edc.vault.hashicorp.token.scheduled-renew-enabled";

    @Setting(value = "The time-to-live (ttl) value of the Hashicorp Vault token in seconds", defaultValue = "300", type = "long")
    public static final String VAULT_TOKEN_TTL = "edc.vault.hashicorp.token.ttl";

    @Setting(value = "The renew buffer of the Hashicorp Vault token in seconds", defaultValue = "30", type = "long")
    public static final String VAULT_TOKEN_RENEW_BUFFER = "edc.vault.hashicorp.token.renew-buffer";

    @Setting(value = "The URL path of the vault's /secret endpoint", defaultValue = VAULT_API_SECRET_PATH_DEFAULT)
    public static final String VAULT_API_SECRET_PATH = "edc.vault.hashicorp.api.secret.path";

    @Setting(value = "The path of the folder that the secret is stored in", required = false, defaultValue = "")
    public static final String VAULT_FOLDER_PATH = "edc.vault.hashicorp.folder";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private HashicorpVaultClient client;
    private HashicorpVaultTokenRenewTask tokenRenewalTask;
    private Monitor monitor;
    private HashicorpVaultSettings settings;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public HashicorpVaultClient hashicorpVaultClient() {
        if (client == null) {
            // the default type manager cannot be used as the Vault is a primordial service loaded at boot
            var mapper = new ObjectMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

            client = new HashicorpVaultClient(
                    httpClient,
                    mapper,
                    monitor,
                    settings);
        }
        return client;
    }

    @Provider
    public Vault hashicorpVault() {
        return new HashicorpVault(hashicorpVaultClient(), monitor);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix(NAME);
        settings = getSettings(context);
        tokenRenewalTask = new HashicorpVaultTokenRenewTask(
                NAME,
                executorInstrumentation,
                hashicorpVaultClient(),
                settings.renewBuffer(),
                monitor);
    }

    @Override
    public void start() {
        if (settings.scheduledTokenRenewEnabled()) {
            tokenRenewalTask.start();
        }
    }

    @Override
    public void shutdown() {
        if (tokenRenewalTask.isRunning()) {
            tokenRenewalTask.stop();
        }
    }

    private HashicorpVaultSettings getSettings(ServiceExtensionContext context) {
        var url = context.getSetting(VAULT_URL, null);
        var healthCheckEnabled = context.getSetting(VAULT_HEALTH_CHECK_ENABLED, VAULT_HEALTH_CHECK_ENABLED_DEFAULT);
        var healthCheckPath = context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);
        var healthStandbyOk = context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
        var token = context.getSetting(VAULT_TOKEN, null);
        var isScheduledTokenRenewEnabled = context.getSetting(VAULT_TOKEN_SCHEDULED_RENEW_ENABLED, VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT);
        var ttl = context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_DEFAULT);
        var renewBuffer = context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        var secretPath = context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);
        var folderPath = context.getSetting(VAULT_FOLDER_PATH, "");

        return HashicorpVaultSettings.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(healthCheckEnabled)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(healthStandbyOk)
                .token(token)
                .scheduledTokenRenewEnabled(isScheduledTokenRenewEnabled)
                .ttl(ttl)
                .renewBuffer(renewBuffer)
                .secretPath(secretPath)
                .folderPath(folderPath)
                .build();
    }
}
