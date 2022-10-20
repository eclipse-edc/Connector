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

class HashicorpVaultConfig {
    private final String vaultUrl;
    private final String vaultToken;

    HashicorpVaultConfig(String vaultUrl, String vaultToken) {
        this.vaultUrl = vaultUrl;
        this.vaultToken = vaultToken;
    }

    public String getVaultUrl() {
        return this.vaultUrl;
    }

    public String getVaultToken() {
        return this.vaultToken;
    }

    public static class Builder {
        private String vaultUrl;
        private String vaultToken;

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

        public HashicorpVaultConfig build() {
            return new HashicorpVaultConfig(vaultUrl, vaultToken);
        }
    }
}
