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

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.http.spi.FallbackFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for enabling the renewal of vault tokens. Facilitates checking if a token
 * is renewable and renewing the token with the provided TTL.
 */
public class HashicorpVaultTokenRenewService {
    
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    
    private static final String TOKEN_LOOK_UP_SELF_PATH = "v1/auth/token/lookup-self";
    private static final String TOKEN_RENEW_SELF_PATH = "v1/auth/token/renew-self";
    private static final List<FallbackFactory> FALLBACK_FACTORIES = List.of(new HashicorpVaultClientFallbackFactory());

    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HashicorpVaultConfig settings;
    private final HashicorpVaultTokenProvider tokenProvider;

    /**
     * Constructor for the HashicorpVaultTokenRenewService.
     *
     * @param httpClient    the HTTP client
     * @param objectMapper  the object mapper
     * @param settings      the configuration for interacting with HashiCorp Vault
     * @param tokenProvider the token provider for retrieving the vault authentication token
     */
    public HashicorpVaultTokenRenewService(@NotNull EdcHttpClient httpClient,
                                           @NotNull ObjectMapper objectMapper,
                                           @NotNull HashicorpVaultConfig settings,
                                           @NotNull HashicorpVaultTokenProvider tokenProvider) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.tokenProvider = tokenProvider;
    }
    
    /**
     * Attempts to look up the current Vault token and returns a boolean indicating if the token is renewable.
     * <p>
     * Will retry in some error cases.
     *
     * @return boolean indicating if the token is renewable
     */
    public Result<Boolean> isTokenRenewable() {
        var uri = HttpUrl.parse(settings.getVaultUrl())
                .newBuilder()
                .addPathSegments(TOKEN_LOOK_UP_SELF_PATH)
                .build();

        var request = new Request.Builder()
                .url(uri)
                .headers(getHeaders())
                .get()
                .build();

        return execute(request, LookupSelfResponse.class)
                .map(LookupSelfResponse::data).map(LookupSelfResponseData::renewable);
    }

    /**
     * Attempts to renew the Vault token with the configured ttl. Note that Vault will not honor the passed
     * ttl (or increment) for periodic tokens. Therefore, the ttl returned by this operation should always be used
     * for further calculations.
     * <p>
     * Will retry in some error cases.
     *
     * @return long representing the remaining ttl of the token in seconds
     */
    public Result<Long> renewToken() {
        var uri = HttpUrl.parse(settings.getVaultUrl())
                .newBuilder()
                .addPathSegments(TOKEN_RENEW_SELF_PATH)
                .build();
        var requestPayload = Map.of("increment", "%ds".formatted(settings.getTtl()));
        var request = new Request.Builder()
                .url(uri)
                .headers(getHeaders())
                .post(createRequestBody(requestPayload))
                .build();

        return execute(request, RenewResponse.class)
                .map(RenewResponse::auth).map(RenewAuthResponse::leaseDuration);
    }

    private <R> Result<R> execute(Request request, Class<R> responseBodyType) {
        try (var response = httpClient.execute(request, FALLBACK_FACTORIES)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                var payload = objectMapper.readValue(responseBody.string(), responseBodyType);
                return Result.success(payload);
            } else {
                return Result.failure("Response failed with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Unexpected exception: %s".formatted(e.getMessage()));
        }
    }

    private Headers getHeaders() {
        var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        headersBuilder.add(VAULT_TOKEN_HEADER, tokenProvider.vaultToken());
        return headersBuilder.build();
    }
    
    private RequestBody createRequestBody(Object requestPayload) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        return RequestBody.create(jsonRepresentation, MEDIA_TYPE_APPLICATION_JSON);
    }

    private record LookupSelfResponse(
            @JsonProperty(required = true)
            LookupSelfResponseData data
    ) {}

    private record LookupSelfResponseData(
            @JsonProperty(defaultValue = "false")
            boolean renewable
    ) {}

    private record RenewResponse(
            @JsonProperty(required = true)
            RenewAuthResponse auth
    ) {}

    private record RenewAuthResponse(
            @JsonProperty("lease_duration")
            long leaseDuration
    ) {}
}
