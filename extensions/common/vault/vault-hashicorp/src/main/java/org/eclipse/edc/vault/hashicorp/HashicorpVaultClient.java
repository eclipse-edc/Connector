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
import com.fasterxml.jackson.databind.JsonMappingException;
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
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponsePayloadData;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalRequestPayload;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewalResponsePayload;
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
    private static final String CALL_UNSUCCESSFUL_ERROR_TEMPLATE = "Call unsuccessful: %s";
    private static final String TOKEN_LOOK_UP_SELF_PATH = "/v1/auth/token/lookup-self";
    private static final String TOKEN_RENEW_SELF_PATH = "/v1/auth/token/renew-self";
    private static final int HTTP_CODE_404 = 404;

    @NotNull
    private final HashicorpVaultConfig hashicorpVaultConfig;
    @NotNull
    private final EdcHttpClient httpClient;
    @NotNull
    private final ScheduledExecutorService scheduledExecutorService;
    @NotNull
    private final ObjectMapper objectMapper;
    @NotNull
    private final Monitor monitor;
    private TokenLookUpResponsePayloadData token;

    HashicorpVaultClient(@NotNull HashicorpVaultConfig hashicorpVaultConfig,
                         @NotNull EdcHttpClient httpClient,
                         @NotNull ObjectMapper objectMapper,
                         @NotNull ScheduledExecutorService scheduledExecutorService,
                         @NotNull Monitor monitor) {
        this.hashicorpVaultConfig = hashicorpVaultConfig;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.scheduledExecutorService = scheduledExecutorService;
        this.monitor = monitor;
    }

    HealthCheckResponse doHealthCheck() {
        var healthResponseBuilder = HealthCheckResponse.Builder.newInstance();
        var requestUri = getHealthCheckUrl();
        var request = httpGet(requestUri);

        try (var response = httpClient.execute(request)) {
            var code = response.code();
            healthResponseBuilder.code(code);

            try {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload = objectMapper.readValue(responseBody, HealthCheckResponsePayload.class);
                healthResponseBuilder.payload(responsePayload);
            } catch (JsonMappingException e) {
                // ignore. status code not checked, so it may be possible that no payload was
                // provided
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }

        return healthResponseBuilder.build();
    }


    void inspectToken() {
        if (token.isRootToken()) {
            monitor.warning("Root token detected. Don't use root tokens in production environment!");
        } else {
            if (token.isRenewable()) {
                monitor.info("Token is renewable");
            } else {
                monitor.warning("Token is not renewable");
            }

            if (token.isPeriodicToken()) {
                monitor.info("Token has a renewal period of %s".formatted(token.getPeriod()));
                monitor.warning("Configured time-to-live (ttl) will not be honored for periodic tokens");
            } else {
                monitor.warning("Non-periodic Token will expire at some point. Check 'max_ttl' property inside Vault server configuration file for more information");
            }

            if (token.hasExplicitMaxTimeToLive()) {
                // TODO: Time conversion
                monitor.warning("Token has explicit expiration time at %s".formatted(token.getIssueTime() + token.getExplicitMaxTimeToLive()));
            }
        }
    }

    Result<Void> lookUpToken() {
        var uri = Objects.requireNonNull(HttpUrl.parse(hashicorpVaultConfig.vaultUrl()))
                .newBuilder()
                .addPathSegment(PathUtil.trimLeadingOrEndingSlash(TOKEN_LOOK_UP_SELF_PATH))
                .build();
        var request = httpGet(uri);

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var payload = objectMapper.readValue(responseBody, TokenLookUpResponsePayload.class);
                token = payload.getData();
                return Result.success();
            } else {
                return Result.failure("%d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    boolean isTokenRenewable() {
        return token.isRenewable();
    }

    Result<Void> renewToken() {
        var uri = Objects.requireNonNull(HttpUrl.parse(hashicorpVaultConfig.vaultUrl()))
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(TOKEN_RENEW_SELF_PATH))
                .build();
        var requestPayload = TokenRenewalRequestPayload.Builder
                .newInstance()
                .increment(hashicorpVaultConfig.timeToLive())
                .build();
        var request = httpPost(uri, requestPayload);

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var payload = objectMapper.readValue(responseBody, TokenRenewalResponsePayload.class);
                payload.getWarnings().forEach(monitor::warning);
                token.setTimeToLive(payload.getAuth().getTimeToLive());
                return Result.success();
            } else {
                return Result.failure("%d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    void scheduleNextTokenRenewal() {
        var delay = token.getTimeToLive() - hashicorpVaultConfig.renewBuffer();

        scheduledExecutorService.schedule(
                () -> {
                    var tokenRenewalResult = renewToken();

                    if (tokenRenewalResult.succeeded()) {
                        monitor.info("Token was renewed successfully");
                        scheduleNextTokenRenewal();
                    } else {
                        monitor.warning("Failed to renew token: %s".formatted(tokenRenewalResult.getFailureDetail()));
                    }
                },
                delay,
                TimeUnit.SECONDS);

        monitor.info("Next token renewal scheduled in %d seconds".formatted(delay));
    }

    Result<String> getSecretValue(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var request = httpGet(requestUri);

        try (var response = httpClient.execute(request)) {

            if (response.isSuccessful()) {
                if (response.code() == HTTP_CODE_404) {
                    return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Secret not found"));
                }

                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Response body empty"));
                }
                var payload = objectMapper.readValue(responseBody.string(), GetEntryResponsePayload.class);
                var value = payload.getData().getData().get(VAULT_DATA_ENTRY_NAME);

                return Result.success(value);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }

        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    Result<CreateEntryResponsePayload> setSecret(@NotNull String key, @NotNull String value) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
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
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    Result<Void> destroySecret(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestUri).headers(headers).delete().build();

        try (var response = httpClient.execute(request)) {
            return response.isSuccessful() || response.code() == HTTP_CODE_404
                    ? Result.success()
                    : Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    private HttpUrl getHealthCheckUrl() {
        final var vaultHealthPath = hashicorpVaultConfig.vaultApiHealthPath();
        final var isVaultHealthStandbyOk = hashicorpVaultConfig.isHealthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return Objects.requireNonNull(HttpUrl.parse(hashicorpVaultConfig.vaultUrl()))
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

        var vaultApiPath = hashicorpVaultConfig.getSecretPath();

        return Objects.requireNonNull(HttpUrl.parse(hashicorpVaultConfig.vaultUrl()))
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
                .headers(getHeaders())
                .get()
                .build();
    }

    @NotNull
    private Request httpPost(HttpUrl requestUri, Object requestBody) {
        return new Request.Builder()
                .url(requestUri)
                .headers(getHeaders())
                .post(createRequestBody(requestBody))
                .build();
    }

    @NotNull
    private Headers getHeaders() {
        var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        if (hashicorpVaultConfig.vaultToken() != null) {
            headersBuilder.add(VAULT_TOKEN_HEADER, hashicorpVaultConfig.vaultToken());
        }
        return headersBuilder.build();
    }

    private RequestBody createRequestBody(Object requestPayload) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return RequestBody.create(jsonRepresentation, MEDIA_TYPE_APPLICATION_JSON);
    }

}
