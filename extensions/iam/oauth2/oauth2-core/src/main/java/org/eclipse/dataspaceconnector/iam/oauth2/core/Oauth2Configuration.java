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

package org.eclipse.dataspaceconnector.iam.oauth2.core;

import org.eclipse.dataspaceconnector.iam.oauth2.core.identity.Oauth2ServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

/**
 * Configuration values and dependencies for {@link Oauth2ServiceImpl}.
 */
public class Oauth2Configuration {

    private final String tokenUrl;
    private final String clientId;
    private final PrivateKeyResolver privateKeyResolver;
    private final CertificateResolver certificateResolver;
    private final PublicKeyResolver identityProviderKeyResolver;
    private final String privateKeyAlias;
    private final String publicCertificateAlias;
    private final String providerAudience;
    private final int notBeforeValidationLeeway;

    public Oauth2Configuration(String tokenUrl, String clientId, PrivateKeyResolver privateKeyResolver,
                               CertificateResolver certificateResolver, PublicKeyResolver identityProviderKeyResolver,
                               String privateKeyAlias, String publicCertificateAlias, String providerAudience,
                               int notBeforeValidationLeeway) {

        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.privateKeyResolver = privateKeyResolver;
        this.certificateResolver = certificateResolver;
        this.identityProviderKeyResolver = identityProviderKeyResolver;
        this.privateKeyAlias = privateKeyAlias;
        this.publicCertificateAlias = publicCertificateAlias;
        this.providerAudience = providerAudience;
        this.notBeforeValidationLeeway = notBeforeValidationLeeway;
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

    public int getNotBeforeValidationLeeway() {
        return notBeforeValidationLeeway;
    }

    public static class Builder {
        private String tokenUrl;
        private String clientId;
        private PrivateKeyResolver privateKeyResolver;
        private CertificateResolver certificateResolver;
        private PublicKeyResolver identityProviderKeyResolver;
        private String privateKeyAlias;
        private String publicCertificateAlias;
        private String providerAudience;
        private int notBeforeValidationLeeway;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            this.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder privateKeyResolver(PrivateKeyResolver privateKeyResolver) {
            this.privateKeyResolver = privateKeyResolver;
            return this;
        }

        /**
         * Resolves this runtime's certificate containing its public key.
         */
        public Builder certificateResolver(CertificateResolver certificateResolver) {
            this.certificateResolver = certificateResolver;
            return this;
        }

        /**
         * Resolves the certificate containing the identity provider's public key.
         */
        public Builder identityProviderKeyResolver(PublicKeyResolver identityProviderKeyResolver) {
            this.identityProviderKeyResolver = identityProviderKeyResolver;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            this.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicCertificateAlias(String publicCertificateAlias) {
            this.publicCertificateAlias = publicCertificateAlias;
            return this;
        }

        public Builder providerAudience(String providerAudience) {
            this.providerAudience = providerAudience;
            return this;
        }

        public Builder notBeforeValidationLeeway(int notBeforeValidationLeeway) {
            this.notBeforeValidationLeeway = notBeforeValidationLeeway;
            return this;
        }

        public Oauth2Configuration build() {
            return new Oauth2Configuration(tokenUrl, clientId, privateKeyResolver, certificateResolver,
                    identityProviderKeyResolver, privateKeyAlias, publicCertificateAlias, providerAudience,
                    notBeforeValidationLeeway);
        }
    }
}
