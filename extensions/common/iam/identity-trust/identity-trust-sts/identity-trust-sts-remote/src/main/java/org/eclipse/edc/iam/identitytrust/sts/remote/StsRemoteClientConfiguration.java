/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.remote;

import java.util.Objects;

/**
 * Configuration of the OAuth2 client
 */
public class StsRemoteClientConfiguration {

    private String tokenUrl;
    private String clientId;
    private String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public static class Builder {
        private final StsRemoteClientConfiguration configuration = new StsRemoteClientConfiguration();

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            configuration.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            configuration.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            configuration.clientSecret = clientSecret;
            return this;
        }

        public StsRemoteClientConfiguration build() {
            Objects.requireNonNull(configuration.clientId, "Client id");
            Objects.requireNonNull(configuration.clientSecret, "Client secret");
            Objects.requireNonNull(configuration.tokenUrl, "Token Url");
            return configuration;
        }
    }
}
