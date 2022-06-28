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

    HashicorpVaultClientConfig(String vaultUrl, String vaultToken, Duration timeout) {
        this.vaultUrl = vaultUrl;
        this.vaultToken = vaultToken;
        this.timeout = timeout;
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

    public static class Builder {
        private String vaultUrl;
        private String vaultToken;
        private Duration timeout;

        Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder vaultUrl(String vaultUrl) {
            this.vaultUrl = vaultUrl;
            return this;
        }

        public Builder vaultToken(String vaultToken) {
            this.vaultToken = vaultToken;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public HashicorpVaultClientConfig build() {
            return new HashicorpVaultClientConfig(vaultUrl, vaultToken, timeout);
        }
    }
}
