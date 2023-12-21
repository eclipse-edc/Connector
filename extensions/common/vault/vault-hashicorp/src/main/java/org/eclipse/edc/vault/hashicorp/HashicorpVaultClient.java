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
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryRequestPayload;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponsePayloadToken;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalRequestPayload;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalResponsePayloadToken;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class HashicorpVaultClient {
    static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final String TOKEN_LOOK_UP_SELF_PATH = "v1/auth/token/lookup-self";
    private static final String TOKEN_RENEW_SELF_PATH = "v1/auth/token/renew-self";
    private static final int HTTP_CODE_404 = 404;
    private static final String DELIMITER = ", ";

    private final Headers headers;

    @NotNull
    private final EdcHttpClient httpClient;
    @NotNull
    private final ScheduledExecutorService scheduledExecutorService;
    @NotNull
    private final ObjectMapper objectMapper;
    @NotNull
    private final Monitor monitor;
    @NotNull
    private final HashicorpVaultConfigValues configValues;

    HashicorpVaultClient(@NotNull EdcHttpClient httpClient,
                         @NotNull ObjectMapper objectMapper,
                         @NotNull ScheduledExecutorService scheduledExecutorService,
                         @NotNull Monitor monitor,
                         @NotNull HashicorpVaultConfigValues configValues) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.scheduledExecutorService = scheduledExecutorService;
        this.monitor = monitor;
        this.configValues = configValues;
        this.headers = getHeaders();
    }

    Result<HealthCheckResponse> doHealthCheck() {
        var requestUri = getHealthCheckUrl();
        var request = httpGet(requestUri);

        int code;
        String payload;

        try (var response = httpClient.execute(request)) {
            code = response.code();
            var responseBody = response.body();
            if (responseBody == null) {
                return Result.failure("Healthcheck returned empty response body");
            }
            payload = responseBody.string();
        } catch (IOException e) {
            monitor.warning("Failed to perform healthcheck with reason: %s".formatted(e.getMessage()));
            return Result.failure("Failed to perform healthcheck");
        }

        var healthCheckResponseBuilder = HealthCheckResponse.Builder.newInstance();

        try {
            var responsePayload = objectMapper.readValue(payload, HealthCheckResponsePayload.class);
            healthCheckResponseBuilder
                    .code(code)
                    .payload(responsePayload);
        } catch (IOException e) {
            healthCheckResponseBuilder.code(code);
        }

        return Result.success(healthCheckResponseBuilder.build());
    }

    Result<TokenLookUpResponsePayloadToken> lookUpToken() {
        var uri = Objects.requireNonNull(HttpUrl.parse(configValues.url()))
                .newBuilder()
                .addPathSegment(TOKEN_LOOK_UP_SELF_PATH)
                .build();
        var request = httpGet(uri);

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token look up returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), TokenLookUpResponsePayload.class);
                return Result.success(payload.getToken());
            } else {
                return Result.failure("Token look up failed with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            monitor.warning("Failed to look up token with reason: %s".formatted(e.getMessage()));
            return Result.failure("Token look up failed");
        }
    }

    Result<TokenRenewalResponsePayloadToken> renewToken() {
        var uri = Objects.requireNonNull(HttpUrl.parse(configValues.url()))
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(TOKEN_RENEW_SELF_PATH))
                .build();
        var requestPayload = TokenRenewalRequestPayload.Builder
                .newInstance()
                .increment(configValues.timeToLive())
                .build();
        var request = httpPost(uri, requestPayload);

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token renew returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), TokenRenewalResponsePayload.class);
                if (!payload.getWarnings().isEmpty()) {
                    var warnings = String.join(DELIMITER, payload.getWarnings());
                    monitor.warning("Token renew returned: " + warnings);
                }
                return Result.success(payload.getToken());
            } else {
                return Result.failure("Token renew failed with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            monitor.warning("Failed to renew token: %s".formatted(e.getMessage()));
            return Result.failure("Failed to renew token");
        }
    }

    void scheduleNextTokenRenewal(long timeToLive) {
        var delay = timeToLive - configValues.renewBuffer();

        scheduledExecutorService.schedule(() -> {
            var tokenRenewResult = renewToken();

            if (tokenRenewResult.succeeded()) {
                var token = tokenRenewResult.getContent();
                scheduleNextTokenRenewal(token.getTimeToLive());
            } else {
                monitor.warning("Scheduled token renewal failed: %s".formatted(tokenRenewResult.getFailureDetail()));
            }
        }, delay, TimeUnit.SECONDS);
    }

    Result<String> getSecretValue(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var request = httpGet(requestUri);

        try (var response = httpClient.execute(request)) {

            if (response.isSuccessful()) {
                if (response.code() == HTTP_CODE_404) {
                    return Result.failure("Secret %s not found".formatted(key));
                }

                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Secret %s response body is empty".formatted(key));
                }
                var payload = objectMapper.readValue(responseBody.string(), GetEntryResponsePayload.class);
                var value = payload.getData().getData().get(VAULT_DATA_ENTRY_NAME);

                return Result.success(value);
            } else {
                return Result.failure("Failed to get secret %s with status %d".formatted(key, response.code()));
            }

        } catch (IOException e) {
            monitor.warning("Failed to get secret %s with reason: %s".formatted(key, e.getMessage()));
            return Result.failure("Failed to get secret");
        }
    }

    Result<CreateEntryResponsePayload> setSecret(@NotNull String key, @NotNull String value) {
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
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload =
                        objectMapper.readValue(responseBody, CreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure("Failed to set secret %s with status %d".formatted(key, response.code()));
            }
        } catch (IOException e) {
            monitor.warning("Failed to set secret %s with reason: %s".formatted(key, e.getMessage()));
            return Result.failure("Failed to set secret");
        }
    }

    Result<Void> destroySecret(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var request = new Request.Builder().url(requestUri).headers(headers).delete().build();

        try (var response = httpClient.execute(request)) {
            return response.isSuccessful() || response.code() == HTTP_CODE_404
                    ? Result.success()
                    : Result.failure("Failed to destroy secret %s with status %d".formatted(key, response.code()));
        } catch (IOException e) {
            monitor.warning("Failed to destroy secret %s with reason: %s".formatted(key, e.getMessage()));
            return Result.failure("Failed to destroy secret");
        }
    }

    private HttpUrl getHealthCheckUrl() {
        final var vaultHealthPath = configValues.healthCheckPath();
        final var isVaultHealthStandbyOk = configValues.healthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return Objects.requireNonNull(HttpUrl.parse(configValues.url()))
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

        var vaultApiPath = configValues.secretPath();

        return Objects.requireNonNull(HttpUrl.parse(configValues.url()))
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
                .addPathSegment(entryType)
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
        headersBuilder.add(VAULT_TOKEN_HEADER, configValues.token());
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
}
