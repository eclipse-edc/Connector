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

package org.eclipse.edc.vault.hashicorp.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.HashicorpVaultSettings;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * This implementation takes a client-ID and client-secret and uses them to obtain a vault token from Hashicorp Vault.
 */
public class HashicorpJwtTokenProvider implements HashicorpVaultTokenProvider {
    private static final String DEFAULT_ROLE = "participant";
    public static final String JWT_LOGIN_PATH = "v1/auth/jwt/login";
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String vaultUrl;

    private String role = DEFAULT_ROLE;
    private ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private EdcHttpClient httpClient;

    private HashicorpJwtTokenProvider() {
    }

    /**
     * Gets a vault token using the provided vault config. If the {@link HashicorpVaultSettings#credentials()} contains a token,
     * the token is simply returned. Otherwise, a JWT is retrieved from the IdP using the provided credentials.
     *
     */
    @Override
    public String vaultToken() {
        // get JWT from IdP
        var accessToken = getAccessToken();

        // use JWT to get vault token

        var parsedUrl = HttpUrl.parse(vaultUrl);
        if (parsedUrl == null) {
            throw new EdcException("Failed to parse vault url '%s'".formatted(tokenUrl));
        }
        var requestUri = parsedUrl.newBuilder(JWT_LOGIN_PATH).build();

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
                throw new EdcException("Failed to obtain vault token, Vault responded with code '%s', message: '%s'".formatted(response.code(), response.body().string()));
            }
            var json = objectMapper.readValue(response.body().string(), JsonNode.class);
            return json.path("auth").path("client_token").asText();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    /**
     * If JWT authentication is used, this method will retrieve the JWT from the IdP using the provided credentials.
     * Credentials must contain a clientId and clientSecret.
     */
    private String getAccessToken() {

        // OAuth Credentials are provided
        var requestUri = HttpUrl.parse(tokenUrl);
        if (requestUri == null) {
            throw new EdcException("Failed to parse vault url '%s'".formatted(tokenUrl));
        }
        var request = new Request.Builder()
                .url(requestUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(RequestBody.create(
                        "grant_type=client_credentials" +
                                "&client_id=" + clientId +
                                "&client_secret=" + clientSecret,
                        MediaType.parse("application/x-www-form-urlencoded")))
                .build();

        try (var response = httpClient.execute(request)) {
            if (!response.isSuccessful()) {
                throw new EdcException("Failed to obtain JWT access token");
            }
            var json = objectMapper.readValue(response.body().string(), JsonNode.class);
            return json.path("access_token").asText();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public static final class Builder {
        private final HashicorpJwtTokenProvider tokenProvider;

        private Builder(HashicorpJwtTokenProvider instance) {
            tokenProvider = instance;
        }

        public static Builder newInstance() {
            return new Builder(new HashicorpJwtTokenProvider());
        }

        public Builder clientId(String clientId) {
            this.tokenProvider.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.tokenProvider.clientSecret = clientSecret;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.tokenProvider.tokenUrl = tokenUrl;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.tokenProvider.objectMapper = objectMapper;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            this.tokenProvider.httpClient = httpClient;
            return this;
        }

        public Builder role(String role) {
            this.tokenProvider.role = role;
            return this;
        }

        public Builder vaultUrl(String vaultUrl) {
            this.tokenProvider.vaultUrl = vaultUrl;
            return this;
        }

        public HashicorpJwtTokenProvider build() {
            requireNonNull(tokenProvider.httpClient, "HttpClient must be provided to use OAuth2 authentication");
            requireNonNull(tokenProvider.clientId, "clientId must be provided to use OAuth2 authentication");
            requireNonNull(tokenProvider.clientSecret, "clientSecret must be provided to use OAuth2 authentication");
            requireNonNull(tokenProvider.tokenUrl, "tokenUrl must be provided to use OAuth2 authentication");
            requireNonNull(tokenProvider.objectMapper, "objectMapper cannot be 'null' with OAuth2 authentication");
            requireNonNull(tokenProvider.role, "'role' cannot be 'null' with OAuth2 authentication");
            requireNonNull(tokenProvider.vaultUrl, "'vaultUrl' cannot be 'null' with OAuth2 authentication");
            return tokenProvider;
        }
    }
}
