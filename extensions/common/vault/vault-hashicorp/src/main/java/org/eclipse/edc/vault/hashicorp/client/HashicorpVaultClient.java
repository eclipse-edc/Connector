/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.http.spi.FallbackFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryRequestPayload;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is a client implementation for interacting with Hashicorp Vault.
 */
public class HashicorpVaultClient {
    private static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final String TOKEN_LOOK_UP_SELF_PATH = "v1/auth/token/lookup-self";
    private static final String TOKEN_RENEW_SELF_PATH = "v1/auth/token/renew-self";
    private static final List<FallbackFactory> FALLBACK_FACTORIES = List.of(new HashicorpVaultClientFallbackFactory());
    private static final int HTTP_CODE_404 = 404;
    private static final String DATA_KEY = "data";
    private static final String RENEWABLE_KEY = "renewable";
    private static final String AUTH_KEY = "auth";
    private static final String LEASE_DURATION_KEY = "lease_duration";
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final String INCREMENT_SECONDS_FORMAT = "%ds";
    private static final String INCREMENT_KEY = "increment";

    private final EdcHttpClient httpClient;
    private final Headers headers;
    private final ObjectMapper objectMapper;
    private final HashicorpVaultSettings settings;
    private final HttpUrl healthCheckUrl;
    private final Monitor monitor;

    public HashicorpVaultClient(@NotNull EdcHttpClient httpClient,
                                @NotNull ObjectMapper objectMapper,
                                @NotNull Monitor monitor,
                                @NotNull HashicorpVaultSettings settings) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
        this.settings = settings;
        this.headers = getHeaders();
        this.healthCheckUrl = getHealthCheckUrl();
    }

    public Result<Void> doHealthCheck() {
        var request = httpGet(healthCheckUrl);

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                return Result.success();
            }

            var code = response.code();
            var errMsg = switch (code) {
                case 429 -> "Vault is in standby";
                case 472 -> "Vault is in recovery mode";
                case 473 -> "Vault is in performance standby";
                case 501 -> "Vault is not initialized";
                case 503 -> "Vault is sealed";
                default -> "Vault returned unspecified code %d".formatted(code);
            };
            var body = response.body();
            if (body == null) {
                return Result.failure("Healthcheck returned empty response body");
            }
            return Result.failure("Vault is not available. Reason: %s, additional information: %s".formatted(errMsg, body.string()));
        } catch (IOException e) {
            return Result.failure("Failed to perform healthcheck with reason: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Attempts to look up the Vault token defined in the configurations and returns a boolean indicating if the token
     * is renewable.
     * <p>
     * Will retry in some error cases.
     *
     * @return boolean indicating if the token is renewable
     */
    public Result<Boolean> isTokenRenewable() {
        var uri = settings.url()
                .newBuilder()
                .addPathSegment(TOKEN_LOOK_UP_SELF_PATH)
                .build();
        var request = httpGet(uri);

        try (var response = httpClient.execute(request, FALLBACK_FACTORIES)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token look up returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), MAP_TYPE_REFERENCE);
                var parseRenewableResult = parseRenewable(payload);
                if (parseRenewableResult.failed()) {
                    return Result.failure("Token look up response could not be parsed: %s".formatted(parseRenewableResult.getFailureDetail()));
                }
                var isRenewable = parseRenewableResult.getContent();
                return Result.success(isRenewable);
            } else {
                return Result.failure("Token look up failed with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Failed to look up token with reason: %s".formatted(e.getMessage()));
        }
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
        var uri = settings.url()
                .newBuilder()
                .addPathSegments(TOKEN_RENEW_SELF_PATH)
                .build();
        var requestPayload = getTokenRenewRequestPayload();
        var request = httpPost(uri, requestPayload);

        try (var response = httpClient.execute(request, FALLBACK_FACTORIES)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token renew returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), MAP_TYPE_REFERENCE);
                var parseTtlResult = parseTtl(payload);
                if (parseTtlResult.failed()) {
                    return Result.failure("Token renew response could not be parsed: %s".formatted(parseTtlResult.getFailureDetail()));
                }
                var ttl = parseTtlResult.getContent();
                return Result.success(ttl);
            } else {
                return Result.failure("Token renew failed with status: %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Failed to renew token with reason: %s".formatted(e.getMessage()));
        }
    }

    public Result<String> getSecretValue(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var request = httpGet(requestUri);

        try (var response = httpClient.execute(request)) {

            if (response.isSuccessful()) {
                if (response.code() == HTTP_CODE_404) {
                    return Result.failure("Secret not found");
                }

                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Secret response body is empty");
                }
                var payload = objectMapper.readValue(responseBody.string(), GetEntryResponsePayload.class);
                var value = payload.getData().getData().get(VAULT_DATA_ENTRY_NAME);

                return Result.success(value);
            } else {
                return Result.failure("Failed to get secret with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Failed to get secret with reason: %s".formatted(e.getMessage()));
        }
    }

    public Result<CreateEntryResponsePayload> setSecret(@NotNull String key, @NotNull String value) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var requestPayload = CreateEntryRequestPayload.Builder.newInstance()
                .data(Collections.singletonMap(VAULT_DATA_ENTRY_NAME, value))
                .build();
        var request = new Request.Builder()
                .url(requestUri)
                .headers(headers)
                .post(createRequestBody(requestPayload))
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    return Result.failure("Setting secret returned empty body");
                }
                var responseBody = response.body().string();
                var responsePayload =
                        objectMapper.readValue(responseBody, CreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure("Failed to set secret with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Failed to set secret with reason: %s".formatted(e.getMessage()));
        }
    }

    public Result<Void> destroySecret(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var request = new Request.Builder().url(requestUri).headers(headers).delete().build();

        try (var response = httpClient.execute(request)) {
            return response.isSuccessful() || response.code() == HTTP_CODE_404
                    ? Result.success()
                    : Result.failure("Failed to destroy secret with status %d".formatted(response.code()));
        } catch (IOException e) {
            return Result.failure("Failed to destroy secret with reason: %s".formatted(e.getMessage()));
        }
    }

    @NotNull
    private HttpUrl getHealthCheckUrl() {
        var vaultHealthPath = settings.healthCheckPath();
        var isVaultHealthStandbyOk = settings.healthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return settings.url()
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultHealthPath))
                .addQueryParameter("standbyok", isVaultHealthStandbyOk ? "true" : "false")
                .addQueryParameter("perfstandbyok", isVaultHealthStandbyOk ? "true" : "false")
                .build();
    }

    private HttpUrl getSecretUrl(String key, String entryType) {
        key = URLEncoder.encode(key, StandardCharsets.UTF_8);

        // restore '/' characters to allow subdirectories
        key = key.replace("%2F", "/");

        var vaultApiPath = settings.secretPath();
        var folderPath = settings.getFolderPath();

        if (folderPath == null) {
            return settings.url()
                    .newBuilder()
                    .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
                    .addPathSegment(entryType)
                    .addPathSegments(key)
                    .build();
        }

        return settings.url()
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
                .addPathSegment(entryType)
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(folderPath))
                .addPathSegments(key)
                .build();
    }

    @NotNull
    private Request httpGet(HttpUrl requestUri) {
        return new Request.Builder()
                .url(requestUri)
                .headers(headers)
                .get()
                .build();
    }

    @NotNull
    private Request httpPost(HttpUrl requestUri, Object requestBody) {
        return new Request.Builder()
                .url(requestUri)
                .headers(headers)
                .post(createRequestBody(requestBody))
                .build();
    }

    @NotNull
    private Headers getHeaders() {
        var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        headersBuilder.add(VAULT_TOKEN_HEADER, settings.token());
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

    private Map<String, String> getTokenRenewRequestPayload() {
        return Map.of(INCREMENT_KEY, INCREMENT_SECONDS_FORMAT.formatted(settings.ttl()));
    }

    private Result<Boolean> parseRenewable(Map<String, Object> map) {
        try {
            var data = objectMapper.convertValue(getValueFromMap(map, DATA_KEY), new TypeReference<Map<String, Object>>() {
            });
            var isRenewable = objectMapper.convertValue(getValueFromMap(data, RENEWABLE_KEY), Boolean.class);
            return Result.success(isRenewable);
        } catch (IllegalArgumentException e) {
            var errMsgFormat = "Failed to parse renewable flag from token look up response %s with reason: %s";
            monitor.warning(errMsgFormat.formatted(map, e.getStackTrace()));
            return Result.failure(errMsgFormat.formatted(map, e.getMessage()));
        }
    }

    private Result<Long> parseTtl(Map<String, Object> map) {
        try {
            var auth = objectMapper.convertValue(getValueFromMap(map, AUTH_KEY), new TypeReference<Map<String, Object>>() {
            });
            var ttl = objectMapper.convertValue(getValueFromMap(auth, LEASE_DURATION_KEY), Long.class);
            return Result.success(ttl);
        } catch (IllegalArgumentException e) {
            var errMsgFormat = "Failed to parse ttl from token renewal response %s with reason: %s";
            monitor.warning(errMsgFormat.formatted(map, e.getStackTrace()));
            return Result.failure(errMsgFormat.formatted(map, e.getMessage()));
        }
    }

    private Object getValueFromMap(Map<String, Object> map, String key) {
        var value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key '%s' does not exist".formatted(key));
        }
        return value;
    }
}
