/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static org.eclipse.edc.util.string.StringUtils.isNullOrEmpty;

/**
 * POJO to contain Hashicorp Vault credentials. Must either contain a token or (clientId, clientSecret and tokenUrl) or a vault token.
 * Containing neither is invalid, and containing both is invalid.
 */
@JsonDeserialize(builder = HashicorpVaultCredentials.Builder.class)
public final class HashicorpVaultCredentials {
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String token;

    private HashicorpVaultCredentials() {
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getToken() {
        return token;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final HashicorpVaultCredentials credentials;

        private Builder() {
            credentials = new HashicorpVaultCredentials();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder clientId(String clientId) {
            this.credentials.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.credentials.clientSecret = clientSecret;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.credentials.tokenUrl = tokenUrl;
            return this;
        }

        public Builder token(String token) {
            this.credentials.token = token;
            return this;
        }

        public HashicorpVaultCredentials build() {
            var hasToken = credentials.token != null;
            var hasClientCredentials = !isNullOrEmpty(credentials.clientId) && !isNullOrEmpty(credentials.clientSecret) && !isNullOrEmpty(credentials.tokenUrl);
            var hasPartialClientCredentials = !isNullOrEmpty(credentials.clientId) || !isNullOrEmpty(credentials.clientSecret) || !isNullOrEmpty(credentials.tokenUrl);
            if (!hasToken && !hasClientCredentials) {
                throw new IllegalArgumentException("Either token or (clientId, clientSecret and tokenUrl) must be provided");
            }
            if (hasToken && hasClientCredentials) {
                throw new IllegalArgumentException("Either token or (clientId, clientSecret and tokenUrl) must be provided, not both");
            }
            if (hasToken && hasPartialClientCredentials) {
                throw new IllegalArgumentException("Partial configuration for (clientId, clientSecret and tokenUrl) is not allowed when token is provided");
            }
            return credentials;
        }
    }
}
