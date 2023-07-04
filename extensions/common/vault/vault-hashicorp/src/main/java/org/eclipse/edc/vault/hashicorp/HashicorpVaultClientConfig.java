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
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_STANDBY_OK;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TIMEOUT_SECONDS;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TIMEOUT_SECONDS_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_URL;

class HashicorpVaultClientConfig {


    private String vaultUrl;
    private String vaultToken;
    private String vaultApiSecretPath;
    private String vaultApiHealthPath;
    private Duration timeout;
    private boolean isVaultApiHealthStandbyOk;

    private HashicorpVaultClientConfig() {

    }

    public static HashicorpVaultClientConfig create(ServiceExtensionContext context) {
        var vaultUrl = context.getSetting(VAULT_URL, null);
        if (vaultUrl == null) {
            throw new EdcException(format("Vault URL (%s) must be defined", VAULT_URL));
        }

        var vaultTimeoutSeconds = Math.max(0, context.getSetting(VAULT_TIMEOUT_SECONDS, VAULT_TIMEOUT_SECONDS_DEFAULT));
        var vaultTimeoutDuration = Duration.ofSeconds(vaultTimeoutSeconds);

        var vaultToken = context.getSetting(VAULT_TOKEN, null);

        if (vaultToken == null) {
            throw new EdcException(format("For Vault authentication [%s] is required", VAULT_TOKEN));
        }

        var apiSecretPath = context.getSetting(VAULT_API_SECRET_PATH, VAULT_API_SECRET_PATH_DEFAULT);
        var apiHealthPath = context.getSetting(VAULT_API_HEALTH_PATH, VAULT_API_HEALTH_PATH_DEFAULT);
        var isHealthStandbyOk = context.getSetting(VAULT_HEALTH_CHECK_STANDBY_OK, VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);

        return HashicorpVaultClientConfig.Builder.newInstance()
                .vaultUrl(vaultUrl)
                .vaultToken(vaultToken)
                .vaultApiSecretPath(apiSecretPath)
                .vaultApiHealthPath(apiHealthPath)
                .isVaultApiHealthStandbyOk(isHealthStandbyOk)
                .timeout(vaultTimeoutDuration)
                .build();
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public String vaultToken() {
        return vaultToken;
    }

    public String getVaultApiSecretPath() {
        return vaultApiSecretPath;
    }

    public String vaultApiHealthPath() {
        return vaultApiHealthPath;
    }

    public Duration timeout() {
        return timeout;
    }

    public boolean isVaultApiHealthStandbyOk() {
        return isVaultApiHealthStandbyOk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vaultUrl, vaultToken, vaultApiSecretPath, vaultApiHealthPath, timeout, isVaultApiHealthStandbyOk);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HashicorpVaultClientConfig) obj;
        return Objects.equals(this.vaultUrl, that.vaultUrl) &&
                Objects.equals(this.vaultToken, that.vaultToken) &&
                Objects.equals(this.vaultApiSecretPath, that.vaultApiSecretPath) &&
                Objects.equals(this.vaultApiHealthPath, that.vaultApiHealthPath) &&
                Objects.equals(this.timeout, that.timeout) &&
                this.isVaultApiHealthStandbyOk == that.isVaultApiHealthStandbyOk;
    }

    @Override
    public String toString() {
        return "HashicorpVaultConfig[" +
                "vaultUrl=" + vaultUrl + ", " +
                "vaultToken=" + vaultToken + ", " +
                "vaultApiSecretPath=" + vaultApiSecretPath + ", " +
                "vaultApiHealthPath=" + vaultApiHealthPath + ", " +
                "timeout=" + timeout + ", " +
                "isVaultApiHealthStandbyOk=" + isVaultApiHealthStandbyOk + ']';
    }

    public static class Builder {

        private final HashicorpVaultClientConfig config;

        private Builder() {
            config = new HashicorpVaultClientConfig();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder vaultUrl(String vaultUrl) {
            this.config.vaultUrl = vaultUrl;
            return this;
        }

        public Builder vaultToken(String vaultToken) {
            this.config.vaultToken = vaultToken;
            return this;
        }

        public Builder vaultApiSecretPath(String vaultApiSecretPath) {
            this.config.vaultApiSecretPath = vaultApiSecretPath;
            return this;
        }

        public Builder vaultApiHealthPath(String vaultApiHealthPath) {
            this.config.vaultApiHealthPath = vaultApiHealthPath;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.config.timeout = timeout;
            return this;
        }

        public Builder isVaultApiHealthStandbyOk(boolean isStandbyOk) {
            this.config.isVaultApiHealthStandbyOk = isStandbyOk;
            return this;
        }

        public HashicorpVaultClientConfig build() {
            return config;
        }
    }
}
