/*
 *  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

import static java.util.Objects.requireNonNull;

/**
 * Configuration parameters for the {@link HashicorpVaultHealthService}. This object is not intended to be constructed manually, instead
 * it is constructed via the configuration injection mechanism.
 */
@Settings
public class HashicorpVaultConfig {
    public static final String VAULT_API_HEALTH_PATH_DEFAULT = "/v1/sys/health";
    public static final String VAULT_API_SECRET_PATH_DEFAULT = "/v1/secret";
    public static final String VAULT_API_TRANSIT_PATH_DEFAULT = "/v1/transit";
    public static final boolean VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT = false;
    public static final long VAULT_TOKEN_RENEW_BUFFER_DEFAULT = 30;
    public static final long VAULT_TOKEN_TTL_DEFAULT = 300;
    public static final boolean VAULT_HEALTH_CHECK_ENABLED_DEFAULT = true;
    public static final boolean VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT = true;

    @Setting(description = "The URL of the Hashicorp Vault", key = "edc.vault.hashicorp.url")
    private String vaultUrl;

    @Setting(description = "Whether or not the vault health check is enabled", defaultValue = VAULT_HEALTH_CHECK_ENABLED_DEFAULT + "", key = "edc.vault.hashicorp.health.check.enabled")
    private boolean healthCheckEnabled;

    @Setting(description = "The URL path of the vault's /health endpoint", defaultValue = VAULT_API_HEALTH_PATH_DEFAULT, key = "edc.vault.hashicorp.api.health.check.path")
    private String healthCheckPath;

    @Setting(description = "Specifies if being a standby should still return the active status code instead of the standby status code", defaultValue = VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT + "", key = "edc.vault.hashicorp.health.check.standby.ok")
    private boolean healthStandbyOk;

    @Setting(description = "Whether the automatic token renewal process will be triggered or not. Should be disabled only for development and testing purposes",
            defaultValue = VAULT_TOKEN_SCHEDULED_RENEW_ENABLED_DEFAULT + "", key = "edc.vault.hashicorp.token.scheduled-renew-enabled")
    private boolean scheduledTokenRenewEnabled;

    @Setting(description = "The time-to-live (ttl) value of the Hashicorp Vault token in seconds", defaultValue = VAULT_TOKEN_TTL_DEFAULT + "", key = "edc.vault.hashicorp.token.ttl")
    private long ttl;

    @Setting(description = "The renew buffer of the Hashicorp Vault token in seconds", defaultValue = VAULT_TOKEN_RENEW_BUFFER_DEFAULT + "", key = "edc.vault.hashicorp.token.renew-buffer")
    private long renewBuffer;

    @Setting(description = "The URL path of the vault's /secret endpoint", defaultValue = VAULT_API_SECRET_PATH_DEFAULT, key = "edc.vault.hashicorp.api.secret.path")
    private String secretPath;

    @Setting(description = "The path of the folder that the secret is stored in, relative to VAULT_FOLDER_PATH", required = false, key = "edc.vault.hashicorp.folder")
    private String folderPath;

    public boolean isAllowFallback() {
        return allowFallback;
    }

    @Setting(description = "Allow fallback to default vault partition if vault partitioning is not set up", defaultValue = "true", key = "edc.vault.hashicorp.allow-fallback")
    private boolean allowFallback = true;

    private HashicorpVaultConfig() {
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public String getSecretsEnginePath() {
        return VAULT_API_TRANSIT_PATH_DEFAULT;
    }

    public boolean getHealthStandbyOk() {
        return healthStandbyOk;
    }

    public boolean getScheduledTokenRenewEnabled() {
        return scheduledTokenRenewEnabled;
    }

    public long getTtl() {
        return ttl;
    }

    public long getRenewBuffer() {
        return renewBuffer;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public static class Builder {
        private final HashicorpVaultConfig config;

        private Builder() {
            config = new HashicorpVaultConfig();
            // those default values are helpful, e.g., when deserializing the config from JSON
            config.ttl = VAULT_TOKEN_TTL_DEFAULT;
            config.healthCheckPath = VAULT_API_HEALTH_PATH_DEFAULT;
            config.secretPath = VAULT_API_SECRET_PATH_DEFAULT;
            config.healthStandbyOk = VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT;
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder vaultUrl(String url) {
            requireNonNull(url, "Vault url must not be null");
            config.vaultUrl = url;
            return this;
        }

        public Builder healthCheckEnabled(boolean healthCheckEnabled) {
            config.healthCheckEnabled = healthCheckEnabled;
            return this;
        }

        public Builder healthCheckPath(String healthCheckPath) {
            config.healthCheckPath = healthCheckPath;
            return this;
        }

        public Builder healthStandbyOk(boolean healthStandbyOk) {
            config.healthStandbyOk = healthStandbyOk;
            return this;
        }

        public Builder scheduledTokenRenewEnabled(boolean scheduledTokenRenewEnabled) {
            config.scheduledTokenRenewEnabled = scheduledTokenRenewEnabled;
            return this;
        }

        public Builder ttl(long ttl) {
            config.ttl = ttl;
            return this;
        }

        public Builder renewBuffer(long renewBuffer) {
            config.renewBuffer = renewBuffer;
            return this;
        }

        public Builder secretPath(String secretPath) {
            config.secretPath = secretPath;
            return this;
        }

        public Builder folderPath(String folderPath) {
            config.folderPath = folderPath;
            return this;
        }

        public Builder allowFallback(boolean allowFallback) {
            config.allowFallback = allowFallback;
            return this;
        }

        public HashicorpVaultConfig build() {
            requireNonNull(config.vaultUrl, "Vault url must be valid");
            requireNonNull(config.healthCheckPath, "Vault health check path must not be null");

            if (config.ttl < 5) {
                throw new IllegalArgumentException("Vault token ttl minimum value is 5");
            }

            if (config.renewBuffer >= config.ttl) {
                throw new IllegalArgumentException("Vault token renew buffer value must be less than ttl value");
            }

            return config;
        }
    }
}
