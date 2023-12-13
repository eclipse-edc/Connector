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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Duration;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_API_HEALTH_PATH;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_API_HEALTH_PATH_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_API_SECRET_PATH;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_API_SECRET_PATH_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_ENABLED;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_ENABLED_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_STANDBY_OK;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TIMEOUT_SECONDS;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TIMEOUT_SECONDS_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN_RENEW_BUFFER;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN_RENEW_BUFFER_SECONDS_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN_TTL;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN_TTL_SECONDS_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_URL;

class HashicorpVaultConfig {

    private String url;
    private boolean healthCheckEnabled;
    private String healthCheckPath;
    private boolean healthStandbyOk;
    private Duration timeout;
    private String token;
    private int timeToLive;
    private int renewBuffer;
    private String secretPath;

    private HashicorpVaultConfig() {}

    public static HashicorpVaultConfig create(ServiceExtensionContext context) {
        var url = context.getSetting(VAULT_URL, null);
        if (url == null) {
            throw new EdcException(format("Vault URL [%s] must be defined", VAULT_URL));
        }
        var healthCheckEnabled = context.getSetting(VAULT_HEALTH_CHECK_ENABLED, VAULT_HEALTH_CHECK_ENABLED_DEFAULT);
        var healthCheckPath = context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);
        var healthStandbyOk = context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
        var timeoutSeconds = Math.max(0, context.getSetting(VAULT_TIMEOUT_SECONDS, VAULT_TIMEOUT_SECONDS_DEFAULT));
        var timeoutDuration = Duration.ofSeconds(timeoutSeconds);
        var token = context.getSetting(VAULT_TOKEN, null);
        if (token == null) {
            throw new EdcException(format("For Vault authentication [%s] is required", VAULT_TOKEN));
        }
        var timeToLive = context.getSetting(VAULT_TOKEN_TTL, VAULT_TOKEN_TTL_SECONDS_DEFAULT);
        var renewBuffer = context.getSetting(VAULT_TOKEN_RENEW_BUFFER, VAULT_TOKEN_RENEW_BUFFER_SECONDS_DEFAULT);
        var secretPath = context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);

        return HashicorpVaultConfig.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(healthCheckEnabled)
                .healthCheckPath(healthCheckPath)
                .healthStandbyOk(healthStandbyOk)
                .timeout(timeoutDuration)
                .token(token)
                .timeToLive(timeToLive)
                .renewBuffer(renewBuffer)
                .secretPath(secretPath)
                .build();
    }

    public String vaultUrl() {
        return url;
    }

    public boolean healthCheckEnabled() {
        return healthCheckEnabled;
    }

    public String vaultToken() {
        return token;
    }

    public int timeToLive() {
        return timeToLive;
    }

    public int renewBuffer() {
        return renewBuffer;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public String vaultApiHealthPath() {
        return healthCheckPath;
    }

    public Duration timeout() {
        return timeout;
    }

    public boolean isHealthStandbyOk() {
        return healthStandbyOk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url,
                healthCheckEnabled,
                healthCheckPath,
                healthStandbyOk,
                timeout,
                token,
                timeToLive,
                renewBuffer,
                secretPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HashicorpVaultConfig) obj;
        return Objects.equals(this.url, that.url) &&
                this.healthCheckEnabled == that.healthCheckEnabled &&
                Objects.equals(this.healthCheckPath, that.healthCheckPath) &&
                this.healthStandbyOk == that.healthStandbyOk &&
                Objects.equals(this.timeout, that.timeout) &&
                Objects.equals(this.token, that.token) &&
                Objects.equals(this.timeToLive, that.timeToLive) &&
                Objects.equals(this.renewBuffer, that.renewBuffer) &&
                Objects.equals(this.secretPath, that.secretPath);
    }

    @Override
    public String toString() {
        return "HashicorpVaultConfig[" +
                "url=" + url + ", " +
                "healthCheckEnabled=" + healthCheckEnabled + ", " +
                "healthCheckPath=" + healthCheckPath + ", " +
                "healthStandbyOk=" + healthStandbyOk + ", " +
                "timeout=" + timeout + ", " +
                "vaultToken=" + token + ", " +
                "timeToLive=" + timeToLive + ", " +
                "renewBuffer=" + renewBuffer + ", " +
                "secretPath=" + secretPath + ']';
    }

    public static class Builder {

        private final HashicorpVaultConfig config;

        private Builder() {
            config = new HashicorpVaultConfig();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder url(String vaultUrl) {
            this.config.url = vaultUrl;
            return this;
        }

        public Builder healthCheckEnabled(boolean healthCheckEnabled) {
            this.config.healthCheckEnabled = healthCheckEnabled;
            return this;
        }

        public Builder healthCheckPath(String healthCheckPath) {
            this.config.healthCheckPath = healthCheckPath;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.config.timeout = timeout;
            return this;
        }

        public Builder healthStandbyOk(boolean healthStandbyOk) {
            this.config.healthStandbyOk = healthStandbyOk;
            return this;
        }

        public Builder token(String vaultToken) {
            this.config.token = vaultToken;
            return this;
        }

        public Builder timeToLive(int timeToLive) {
            this.config.timeToLive = timeToLive;
            return this;
        }

        public Builder renewBuffer(int renewBuffer) {
            this.config.renewBuffer = renewBuffer;
            return this;
        }

        public Builder secretPath(String vaultApiSecretPath) {
            this.config.secretPath = vaultApiSecretPath;
            return this;
        }

        public HashicorpVaultConfig build() {
            return config;
        }
    }
}
