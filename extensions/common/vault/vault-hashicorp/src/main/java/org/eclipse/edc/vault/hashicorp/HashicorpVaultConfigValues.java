/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.edc.vault.hashicorp;

import static java.util.Objects.requireNonNull;

/**
 * Value container for {@link HashicorpVaultConfig} configurations.
 */
public class HashicorpVaultConfigValues {

    private String url;
    private boolean healthCheckEnabled;
    private String healthCheckPath;
    private boolean healthStandbyOk;
    private double retryBackoffBase;
    private String token;
    private long ttl;
    private long renewBuffer;
    private String secretPath;

    private HashicorpVaultConfigValues() {}

    public String url() {
        return url;
    }

    public boolean healthCheckEnabled() {
        return healthCheckEnabled;
    }

    public String healthCheckPath() {
        return healthCheckPath;
    }

    public boolean healthStandbyOk() {
        return healthStandbyOk;
    }

    public double retryBackoffBase() {
        return retryBackoffBase;
    }

    public String token() {
        return token;
    }

    public long ttl() {
        return ttl;
    }

    public long renewBuffer() {
        return renewBuffer;
    }

    public String secretPath() {
        return secretPath;
    }

    public static class Builder {
        private final HashicorpVaultConfigValues values;

        private Builder() {
            values = new HashicorpVaultConfigValues();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder url(String url) {
            values.url = url;
            return this;
        }

        public Builder healthCheckEnabled(boolean healthCheckEnabled) {
            values.healthCheckEnabled = healthCheckEnabled;
            return this;
        }

        public Builder healthCheckPath(String healthCheckPath) {
            values.healthCheckPath = healthCheckPath;
            return this;
        }

        public Builder healthStandbyOk(boolean healthStandbyOk) {
            values.healthStandbyOk = healthStandbyOk;
            return this;
        }

        public Builder retryBackoffBase(double retryBackoffBase) {
            values.retryBackoffBase = retryBackoffBase;
            return this;
        }

        public Builder token(String token) {
            values.token = token;
            return this;
        }

        public Builder ttl(long ttl) {
            values.ttl = ttl;
            return this;
        }

        public Builder renewBuffer(long renewBuffer) {
            values.renewBuffer = renewBuffer;
            return this;
        }

        public Builder secretPath(String secretPath) {
            values.secretPath = secretPath;
            return this;
        }

        public HashicorpVaultConfigValues build() {
            requireNonNull(values.url, "Vault url must not be null");
            requireNonNull(values.token, "Vault token must not be null");

            if (values.retryBackoffBase <= 1.0) {
                throw new IllegalArgumentException("Vault retry exponential backoff base be greater than 1");
            }

            if (values.ttl < 5) {
                throw new IllegalArgumentException("Vault token ttl minimum value is 5");
            }

            if (values.renewBuffer >= values.ttl) {
                throw new IllegalArgumentException("Vault token renew buffer value must be less than ttl value");
            }

            return values;
        }
    }
}
