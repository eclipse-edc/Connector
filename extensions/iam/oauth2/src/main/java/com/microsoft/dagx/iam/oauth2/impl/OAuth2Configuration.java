package com.microsoft.dagx.iam.oauth2.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;

/**
 * Configuration values and dependencies for {@link OAuth2ServiceImpl}.
 */
public class OAuth2Configuration {
    private String tokenUrl;
    private String clientId;
    private String privateKeyAlias;
    private String publicCertificateAlias;
    private String providerAudience;

    private PrivateKeyResolver privateKeyResolver;
    private CertificateResolver certificateResolver;
    private PublicKeyResolver identityProviderKeyResolver;

    private ObjectMapper objectMapper;

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

    private OAuth2Configuration() {
    }

    public static class Builder {
        private OAuth2Configuration configuration;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            this.configuration.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            this.configuration.clientId = clientId;
            return this;
        }

        public Builder privateKeyResolver(PrivateKeyResolver resolver) {
            this.configuration.privateKeyResolver = resolver;
            return this;
        }

        /**
         * Resolves this runtime's certificate containing its public key.
         */
        public Builder certificateResolver(CertificateResolver resolver) {
            this.configuration.certificateResolver = resolver;
            return this;
        }

        /**
         * Resolves the certificate containing the identity provider's public key.
         */
        public Builder identityProviderKeyResolver(PublicKeyResolver resolver) {
            this.configuration.identityProviderKeyResolver = resolver;
            return this;
        }

        public Builder privateKeyAlias(String alias) {
            this.configuration.privateKeyAlias = alias;
            return this;
        }

        public Builder publicCertificateAlias(String alias) {
            this.configuration.publicCertificateAlias = alias;
            return this;
        }

        public Builder providerAudience(String audience) {
            this.configuration.providerAudience = audience;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.configuration.objectMapper = objectMapper;
            return this;
        }

        public OAuth2Configuration build() {
            return configuration;
        }

        private Builder() {
            configuration = new OAuth2Configuration();
        }

    }
}
