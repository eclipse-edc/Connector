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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Authenticates against HashiCorp Vault using OAuth2 Token Exchange (RFC 8693).
 * <p>
 * The connector reads its projected Kubernetes ServiceAccount token (the <em>subject token</em>) from a file, exchanges
 * it at the token-exchange service (e.g. {@code jwtlet}) for a participant-scoped JWT, and presents that JWT to Vault's
 * JWT auth method ({@code v1/auth/jwt/login}) to obtain a Vault {@code client_token}. The resulting Vault token is
 * cached until shortly before it expires.
 * <p>
 * Each instance is bound to a single {@code resource} (the participant context id, which becomes the {@code sub} claim
 * of the exchanged token and therefore the vault partition the token is scoped to).
 */
public class HashicorpTokenExchangeProvider implements HashicorpVaultTokenProvider {

    public static final String JWT_LOGIN_PATH = "v1/auth/jwt/login";
    public static final String TOKEN_EXCHANGE_PATH = "token";
    public static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final long REFRESH_BUFFER_SECONDS = 30;

    private String tokenExchangeUrl;
    private String subjectTokenPath;
    private String scope;
    private String audience;
    private String vaultUrl;
    private String role;
    private @Nullable String resource;
    private EdcHttpClient httpClient;
    private ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private String cachedToken;
    private Instant expiresAt = Instant.MIN;

    private HashicorpTokenExchangeProvider() {
    }

    @Override
    public synchronized String vaultToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }

        var subjectToken = readSubjectToken();
        var accessToken = exchangeToken(subjectToken);
        login(accessToken);
        return cachedToken;
    }

    private String readSubjectToken() {
        try {
            return Files.readString(Path.of(subjectTokenPath)).trim();
        } catch (IOException e) {
            throw new EdcException("Failed to read subject token from '%s'".formatted(subjectTokenPath), e);
        }
    }

    private String exchangeToken(String subjectToken) {
        var baseUrl = HttpUrl.parse(tokenExchangeUrl);
        if (baseUrl == null) {
            throw new EdcException("Failed to parse token exchange url '%s'".formatted(tokenExchangeUrl));
        }
        var requestUri = baseUrl.newBuilder().addPathSegment(TOKEN_EXCHANGE_PATH).build();

        var bodyBuilder = new FormBody.Builder()
                .add("grant_type", GRANT_TYPE_TOKEN_EXCHANGE)
                .add("subject_token", subjectToken)
                .add("scope", scope)
                .add("audience", audience);
        if (resource != null) {
            bodyBuilder.add("resource", resource);
        }

        var request = new Request.Builder()
                .url(requestUri)
                .post(bodyBuilder.build())
                .build();

        try (var response = httpClient.execute(request)) {
            if (!response.isSuccessful()) {
                throw new EdcException("Failed to exchange subject token, token exchange service responded with code '%s', message: '%s'"
                        .formatted(response.code(), bodyAsString(response.body())));
            }
            var json = objectMapper.readValue(response.body().string(), JsonNode.class);
            return json.path("access_token").asText();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private void login(String accessToken) {
        var parsedUrl = HttpUrl.parse(vaultUrl);
        if (parsedUrl == null) {
            throw new EdcException("Failed to parse vault url '%s'".formatted(vaultUrl));
        }
        var requestUri = parsedUrl.newBuilder().addPathSegments(JWT_LOGIN_PATH).build();

        var request = new Request.Builder()
                .url(requestUri)
                .post(RequestBody.create("""
                        {
                            "role": "%s",
                            "jwt": "%s"
                        }
                        """.formatted(role, accessToken).getBytes()))
                .build();

        try (var response = httpClient.execute(request)) {
            if (!response.isSuccessful()) {
                throw new EdcException("Failed to obtain vault token, Vault responded with code '%s', message: '%s'"
                        .formatted(response.code(), bodyAsString(response.body())));
            }
            var json = objectMapper.readValue(response.body().string(), JsonNode.class);
            cachedToken = json.path("auth").path("client_token").asText();
            var leaseDuration = json.path("auth").path("lease_duration").asLong();
            var ttl = leaseDuration > REFRESH_BUFFER_SECONDS ? leaseDuration - REFRESH_BUFFER_SECONDS : leaseDuration;
            expiresAt = Instant.now().plusSeconds(ttl);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private String bodyAsString(okhttp3.ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
    }

    public static final class Builder {
        private final HashicorpTokenExchangeProvider provider;

        private Builder(HashicorpTokenExchangeProvider provider) {
            this.provider = provider;
        }

        public static Builder newInstance() {
            return new Builder(new HashicorpTokenExchangeProvider());
        }

        public Builder tokenExchangeUrl(String tokenExchangeUrl) {
            provider.tokenExchangeUrl = tokenExchangeUrl;
            return this;
        }

        public Builder subjectTokenPath(String subjectTokenPath) {
            provider.subjectTokenPath = subjectTokenPath;
            return this;
        }

        public Builder scope(String scope) {
            provider.scope = scope;
            return this;
        }

        public Builder audience(String audience) {
            provider.audience = audience;
            return this;
        }

        public Builder vaultUrl(String vaultUrl) {
            provider.vaultUrl = vaultUrl;
            return this;
        }

        public Builder role(String role) {
            provider.role = role;
            return this;
        }

        public Builder resource(@Nullable String resource) {
            provider.resource = resource;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            provider.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            provider.objectMapper = objectMapper;
            return this;
        }

        public HashicorpTokenExchangeProvider build() {
            requireNonNull(provider.tokenExchangeUrl, "tokenExchangeUrl must be provided for token-exchange authentication");
            requireNonNull(provider.subjectTokenPath, "subjectTokenPath must be provided for token-exchange authentication");
            requireNonNull(provider.scope, "scope must be provided for token-exchange authentication");
            requireNonNull(provider.audience, "audience must be provided for token-exchange authentication");
            requireNonNull(provider.vaultUrl, "vaultUrl must be provided for token-exchange authentication");
            requireNonNull(provider.role, "role must be provided for token-exchange authentication");
            requireNonNull(provider.httpClient, "httpClient must be provided for token-exchange authentication");
            requireNonNull(provider.objectMapper, "objectMapper must be provided for token-exchange authentication");
            return provider;
        }
    }
}
