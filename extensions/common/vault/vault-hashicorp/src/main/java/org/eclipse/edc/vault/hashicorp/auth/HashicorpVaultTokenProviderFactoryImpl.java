/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link HashicorpVaultTokenProviderFactory}.
 * <p>
 * Supports two authentication mechanisms, either or both of which may be configured:
 * <ul>
 *     <li>a static vault token, used for the default partition and as a fallback;</li>
 *     <li>token exchange (RFC 8693), used to mint a participant-scoped vault token per partition.</li>
 * </ul>
 * If both are configured the static token is used for the default partition and token exchange for named partitions.
 * The created providers are cached per partition so the (cached) vault token is shared across callers of a partition.
 */
public class HashicorpVaultTokenProviderFactoryImpl implements HashicorpVaultTokenProviderFactory {

    private static final String STATIC_KEY = "static";
    private static final String EXCHANGE_KEY_PREFIX = "exchange:";

    private final ConcurrentMap<String, HashicorpVaultTokenProvider> cache = new ConcurrentHashMap<>();

    private @Nullable String staticToken;
    private boolean tokenExchangeConfigured;
    private String tokenExchangeUrl;
    private String subjectTokenPath;
    private String scope;
    private String audience;
    private String vaultUrl;
    private String role;
    private @Nullable String defaultResource;
    private EdcHttpClient httpClient;
    private ObjectMapper objectMapper;

    private HashicorpVaultTokenProviderFactoryImpl() {
    }

    @Override
    public HashicorpVaultTokenProvider create(@Nullable String resource) {
        if (resource == null) {
            return staticToken != null ? staticProvider() : exchangeProvider(defaultResource);
        }
        if (tokenExchangeConfigured) {
            return exchangeProvider(resource);
        }
        if (staticToken != null) {
            return staticProvider();
        }
        throw new EdcException("No vault authentication configured for partition '%s'".formatted(resource));
    }

    private HashicorpVaultTokenProvider staticProvider() {
        return cache.computeIfAbsent(STATIC_KEY, k -> new HashicorpVaultTokenProviderImpl(staticToken));
    }

    private HashicorpVaultTokenProvider exchangeProvider(@Nullable String resource) {
        if (!tokenExchangeConfigured) {
            throw new EdcException("Token-exchange authentication is not configured");
        }
        return cache.computeIfAbsent(EXCHANGE_KEY_PREFIX + (resource == null ? "" : resource), k ->
                HashicorpTokenExchangeProvider.Builder.newInstance()
                        .tokenExchangeUrl(tokenExchangeUrl)
                        .subjectTokenPath(subjectTokenPath)
                        .scope(scope)
                        .audience(audience)
                        .vaultUrl(vaultUrl)
                        .role(role)
                        .resource(resource)
                        .httpClient(httpClient)
                        .objectMapper(objectMapper)
                        .build());
    }

    public static final class Builder {
        private final HashicorpVaultTokenProviderFactoryImpl factory;

        private Builder(HashicorpVaultTokenProviderFactoryImpl factory) {
            this.factory = factory;
        }

        public static Builder newInstance() {
            return new Builder(new HashicorpVaultTokenProviderFactoryImpl());
        }

        public Builder staticToken(@Nullable String staticToken) {
            factory.staticToken = staticToken;
            return this;
        }

        public Builder tokenExchangeUrl(String tokenExchangeUrl) {
            factory.tokenExchangeUrl = tokenExchangeUrl;
            return this;
        }

        public Builder subjectTokenPath(String subjectTokenPath) {
            factory.subjectTokenPath = subjectTokenPath;
            return this;
        }

        public Builder scope(String scope) {
            factory.scope = scope;
            return this;
        }

        public Builder audience(String audience) {
            factory.audience = audience;
            return this;
        }

        public Builder vaultUrl(String vaultUrl) {
            factory.vaultUrl = vaultUrl;
            return this;
        }

        public Builder role(String role) {
            factory.role = role;
            return this;
        }

        public Builder defaultResource(@Nullable String defaultResource) {
            factory.defaultResource = defaultResource;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            factory.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            factory.objectMapper = objectMapper;
            return this;
        }

        public HashicorpVaultTokenProviderFactoryImpl build() {
            factory.tokenExchangeConfigured = factory.tokenExchangeUrl != null;
            if (factory.staticToken == null && !factory.tokenExchangeConfigured) {
                throw new IllegalArgumentException("Either a static vault token or token-exchange configuration must be provided");
            }
            return factory;
        }
    }
}
