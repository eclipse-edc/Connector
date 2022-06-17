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

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import java.time.Duration;

class HashicorpVaultClientConfig {
  private final String vaultUrl;
  private final String vaultToken;
  private final Duration timeout;

    public HashicorpVaultClientConfig(String vaultUrl, String vaultToken, Duration timeout) {
        this.vaultUrl = vaultUrl;
        this.vaultToken = vaultToken;
        this.timeout = timeout;
    }

    public static HashicorpVaultClientConfigBuilder builder() {
        return new HashicorpVaultClientConfigBuilder();
    }

    public String getVaultUrl() {
        return this.vaultUrl;
    }

    public String getVaultToken() {
        return this.vaultToken;
    }

    public Duration getTimeout() {
        return this.timeout;
    }

    public static class HashicorpVaultClientConfigBuilder {
        private String vaultUrl;
        private String vaultToken;
        private Duration timeout;

        HashicorpVaultClientConfigBuilder() {
        }

        public HashicorpVaultClientConfigBuilder vaultUrl(String vaultUrl) {
            this.vaultUrl = vaultUrl;
            return this;
        }

        public HashicorpVaultClientConfigBuilder vaultToken(String vaultToken) {
            this.vaultToken = vaultToken;
            return this;
        }

        public HashicorpVaultClientConfigBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public HashicorpVaultClientConfig build() {
            return new HashicorpVaultClientConfig(vaultUrl, vaultToken, timeout);
        }

        public String toString() {
            return "HashicorpVaultClientConfig.HashicorpVaultClientConfigBuilder(vaultUrl=" + this.vaultUrl + ", vaultToken=" + this.vaultToken + ", timeout=" + this.timeout + ")";
        }
    }
}
