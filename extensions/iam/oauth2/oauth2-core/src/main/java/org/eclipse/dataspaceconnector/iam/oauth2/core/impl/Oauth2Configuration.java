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

package org.eclipse.dataspaceconnector.iam.oauth2.core.impl;

import org.eclipse.dataspaceconnector.iam.oauth2.spi.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration values and dependencies for {@link Oauth2ServiceImpl}.
 */
public class Oauth2Configuration {

    enum Config {
        TOKEN_URL,
        CLIENT_ID,
        PRIVATE_KEY_ALIAS,
        PUBLIC_CERTIFICATE_ALIAS,
        PROVIDER_AUDIENCE,
        PRIVATE_KEY_RESOLVER,
        CERTIFICATE_RESOLVER,
        IDENTITY_PROVIDER_KEY_RESOLVER
    }

    private final Map<Config, Object> configs;

    private Oauth2Configuration(Map<Config, Object> configs) {
        // check that all configs have been provided
        Arrays.stream(Config.values()).forEach(config -> Objects.requireNonNull(configs.get(config), "Missing Oauth2 config: " + config));
        this.configs = configs;
    }

    public String getTokenUrl() {
        return (String) configs.get(Config.TOKEN_URL);
    }

    public String getClientId() {
        return (String) configs.get(Config.CLIENT_ID);
    }

    public String getPrivateKeyAlias() {
        return (String) configs.get(Config.PRIVATE_KEY_ALIAS);
    }

    public String getPublicCertificateAlias() {
        return (String) configs.get(Config.PUBLIC_CERTIFICATE_ALIAS);
    }

    public String getProviderAudience() {
        return (String) configs.get(Config.PROVIDER_AUDIENCE);
    }

    public PrivateKeyResolver getPrivateKeyResolver() {
        return (PrivateKeyResolver) configs.get(Config.PRIVATE_KEY_RESOLVER);
    }

    public CertificateResolver getCertificateResolver() {
        return (CertificateResolver) configs.get(Config.CERTIFICATE_RESOLVER);
    }

    public PublicKeyResolver getIdentityProviderKeyResolver() {
        return (PublicKeyResolver) configs.get(Config.IDENTITY_PROVIDER_KEY_RESOLVER);
    }

    public static class Builder {
        private final Map<Config, Object> configs = new EnumMap<>(Config.class);

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            configs.put(Config.TOKEN_URL, url);
            return this;
        }

        public Builder clientId(String clientId) {
            configs.put(Config.CLIENT_ID, clientId);
            return this;
        }

        public Builder privateKeyResolver(PrivateKeyResolver resolver) {
            configs.put(Config.PRIVATE_KEY_RESOLVER, resolver);
            return this;
        }

        /**
         * Resolves this runtime's certificate containing its public key.
         */
        public Builder certificateResolver(CertificateResolver resolver) {
            configs.put(Config.CERTIFICATE_RESOLVER, resolver);
            return this;
        }

        /**
         * Resolves the certificate containing the identity provider's public key.
         */
        public Builder identityProviderKeyResolver(PublicKeyResolver resolver) {
            configs.put(Config.IDENTITY_PROVIDER_KEY_RESOLVER, resolver);
            return this;
        }

        public Builder privateKeyAlias(String alias) {
            configs.put(Config.PRIVATE_KEY_ALIAS, alias);
            return this;
        }

        public Builder publicCertificateAlias(String alias) {
            configs.put(Config.PUBLIC_CERTIFICATE_ALIAS, alias);
            return this;
        }

        public Builder providerAudience(String audience) {
            configs.put(Config.PROVIDER_AUDIENCE, audience);
            return this;
        }

        public Oauth2Configuration build() {
            return new Oauth2Configuration(configs);
        }
    }
}
