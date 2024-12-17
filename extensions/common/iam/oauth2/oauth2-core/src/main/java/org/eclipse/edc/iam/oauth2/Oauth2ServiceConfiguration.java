/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       sovity GmbH - added issuedAt leeway
 *
 */

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.iam.oauth2.identity.Oauth2ServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

/**
 * Configuration values and dependencies for {@link Oauth2ServiceImpl}.
 */
@Settings
public class Oauth2ServiceConfiguration {
    static final String ISSUED_AT_LEEWAY = "edc.oauth.validation.issued.at.leeway";
    private static final int DEFAULT_TOKEN_EXPIRATION = 5;

    @Setting(description = "URL to obtain OAuth2 JSON Web Key Sets", key = "edc.oauth.provider.jwks.url", defaultValue = "http://localhost/empty_jwks_url")
    private String jwksUrl;
    @Setting(description = "OAuth2 Token URL", key = "edc.oauth.token.url")
    private String tokenUrl;
    @Setting(description = "OAuth2 client ID", key = "edc.oauth.client.id")
    private String clientId;
    @Setting(description = "Vault alias for the private key", key = "edc.oauth.private.key.alias")
    private String privateKeyAlias;
    @Setting(description = "Vault alias for the certificate", key = "edc.oauth.certificate.alias")
    private String publicCertificateAlias;
    @Setting(description = "outgoing tokens 'aud' claim value, by default it's the connector id", key = "edc.oauth.provider.audience", required = false)
    private String providerAudience;
    @Setting(description = "Leeway in seconds for validating the not before (nbf) claim in the token.", defaultValue = "10", key = "edc.oauth.validation.nbf.leeway")
    private int notBeforeValidationLeeway;
    @Setting(description = "Leeway in seconds for validating the issuedAt claim in the token. By default it is 0 seconds.", defaultValue = "0", key = ISSUED_AT_LEEWAY)
    private int issuedAtLeeway;
    @Setting(description = "incoming tokens 'aud' claim required value, by default it's the provider audience value", key = "edc.oauth.endpoint.audience", required = false)
    private String endpointAudience;

    @Setting(description = "Refresh time for the JWKS, in minutes", key = "edc.oauth.provider.jwks.refresh", defaultValue = "5")
    private int providerJwksRefresh; // in minutes

    @Setting(description = "Token expiration in minutes. By default is 5 minutes", key = "edc.oauth.token.expiration", defaultValue = DEFAULT_TOKEN_EXPIRATION + "")
    private Long tokenExpiration;

    @Setting(description = "Enable the connector to request a token with a specific audience as defined in the RFC-8707.", key = "edc.oauth.token.resource.enabled", defaultValue = "false")
    private boolean tokenResourceEnabled;

    private Oauth2ServiceConfiguration() {

    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public String getPublicCertificateAlias() {
        return publicCertificateAlias;
    }

    public String getProviderAudience() {
        return providerAudience;
    }

    public int getNotBeforeValidationLeeway() {
        return notBeforeValidationLeeway;
    }

    public int getIssuedAtLeeway() {
        return issuedAtLeeway;
    }

    public String getEndpointAudience() {
        return endpointAudience;
    }

    public Long getTokenExpiration() {
        return tokenExpiration;
    }

    public boolean isTokenResourceEnabled() {
        return tokenResourceEnabled;
    }

    public int getProviderJwksRefresh() {
        return providerJwksRefresh;
    }

    public String getJwksUrl() {
        return jwksUrl;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final Oauth2ServiceConfiguration configuration;

        private Builder(Oauth2ServiceConfiguration configuration) {
            this.configuration = configuration;
        }

        public static Builder newInstance() {
            return new Builder(new Oauth2ServiceConfiguration());
        }

        public Builder tokenUrl(String url) {
            configuration.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            configuration.clientId = clientId;
            return this;
        }


        public Builder privateKeyAlias(String privateKeyAlias) {
            configuration.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicCertificateAlias(String publicCertificateAlias) {
            configuration.publicCertificateAlias = publicCertificateAlias;
            return this;
        }

        public Builder providerAudience(String providerAudience) {
            configuration.providerAudience = providerAudience;
            return this;
        }

        public Builder notBeforeValidationLeeway(int notBeforeValidationLeeway) {
            configuration.notBeforeValidationLeeway = notBeforeValidationLeeway;
            return this;
        }

        public Builder issuedAtLeeway(int issuedAtLeeway) {
            configuration.issuedAtLeeway = issuedAtLeeway;
            return this;
        }

        public Builder endpointAudience(String endpointAudience) {
            configuration.endpointAudience = endpointAudience;
            return this;
        }

        public Builder tokenExpiration(long tokenExpiration) {
            configuration.tokenExpiration = tokenExpiration;
            return this;
        }

        public Builder tokenResourceEnabled(boolean tokenResourceEnabled) {
            configuration.tokenResourceEnabled = tokenResourceEnabled;
            return this;
        }

        public Oauth2ServiceConfiguration build() {
            return configuration;
        }
    }
}
