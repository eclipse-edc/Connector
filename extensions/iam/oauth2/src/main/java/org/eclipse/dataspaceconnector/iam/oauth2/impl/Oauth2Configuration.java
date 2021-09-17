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
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

/**
 * Configuration values and dependencies for {@link Oauth2ServiceImpl}.
 */
public class Oauth2Configuration {
    private String tokenUrl;
    private String clientId;
    private String privateKeyAlias;
    private String publicCertificateAlias;
    private String providerAudience;

    private PrivateKeyResolver privateKeyResolver;
    private CertificateResolver certificateResolver;
    private PublicKeyResolver identityProviderKeyResolver;

    private ObjectMapper objectMapper;

    private Oauth2Configuration() {
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

    public PrivateKeyResolver getPrivateKeyResolver() {
        return privateKeyResolver;
    }

    public CertificateResolver getCertificateResolver() {
        return certificateResolver;
    }

    public PublicKeyResolver getIdentityProviderKeyResolver() {
        return identityProviderKeyResolver;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static class Builder {
        private final Oauth2Configuration configuration;

        private Builder() {
            configuration = new Oauth2Configuration();
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

        public Builder privateKeyResolver(PrivateKeyResolver resolver) {
            configuration.privateKeyResolver = resolver;
            return this;
        }

        /**
         * Resolves this runtime's certificate containing its public key.
         */
        public Builder certificateResolver(CertificateResolver resolver) {
            configuration.certificateResolver = resolver;
            return this;
        }

        /**
         * Resolves the certificate containing the identity provider's public key.
         */
        public Builder identityProviderKeyResolver(PublicKeyResolver resolver) {
            configuration.identityProviderKeyResolver = resolver;
            return this;
        }

        public Builder privateKeyAlias(String alias) {
            configuration.privateKeyAlias = alias;
            return this;
        }

        public Builder publicCertificateAlias(String alias) {
            configuration.publicCertificateAlias = alias;
            return this;
        }

        public Builder providerAudience(String audience) {
            configuration.providerAudience = audience;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            configuration.objectMapper = objectMapper;
            return this;
        }

        public Oauth2Configuration build() {
            return configuration;
        }

    }
}
