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

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryRequestPayload;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponse;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewRequest;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HashicorpVaultClient {
    private static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final String TOKEN_LOOK_UP_SELF_PATH = "v1/auth/token/lookup-self";
    private static final String TOKEN_RENEW_SELF_PATH = "v1/auth/token/renew-self";
    private static final int HTTP_CODE_404 = 404;
    private static final String DELIMITER = ", ";
    private static final long INITIAL_TIMEOUT_SECONDS = 30L;
    private static final Set<Integer> NON_RETRYABLE_STATUS_CODES = Set.of(200, 204, 400, 403, 404, 405);

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

    public Result<Void> doHealthCheck() {
        var requestUri = getHealthCheckUrl();
        var request = httpGet(requestUri);

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
            var errMsg = "Failed to perform healthcheck with reason: %s".formatted(e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
        }
    }

    /**
     * Performs the initial token lookup and renewal. Schedules the next token renewal if both operations were successful.
     * <p>
     * The method is executed asynchronously since the lookup and renewal of tokens are retryable. Otherwise, the main
     * program flow would be halted.
     */
    public void scheduleTokenRenewal() {
        scheduledExecutorService.execute(() -> {
            var tokenLookUpResult = lookUpToken(INITIAL_TIMEOUT_SECONDS);

            if (tokenLookUpResult.failed()) {
                monitor.warning("Initial token look up failed with reason: %s".formatted(tokenLookUpResult.getFailureDetail()));
                return;
            }

            var tokenLookUpResponse = tokenLookUpResult.getContent();

            if (tokenLookUpResponse.getData().isRenewable()) {
                var tokenRenewResult = renewToken(tokenLookUpResponse.getData().getTtl());

                if (tokenRenewResult.failed()) {
                    monitor.warning("Initial token renewal failed with reason: %s".formatted(tokenRenewResult.getFailureDetail()));
                    return;
                }

                var tokenRenewResponse = tokenRenewResult.getContent();
                scheduleNextTokenRenewal(tokenRenewResponse.getAuth().getLeaseDuration());
            }
        });
    }

    /**
     * Attempts to look up the Vault token defined in the configurations.
     * <p>
     * Will retry in some error cases.
     *
     * @param retryTimeout the retry timeout in seconds. Set this to 0 for no retries.
     * @return the result of the token lookup operation
     */
    public Result<TokenLookUpResponse> lookUpToken(long retryTimeout) {
        var uri = getVaultUrl()
                .newBuilder()
                .addPathSegment(TOKEN_LOOK_UP_SELF_PATH)
                .build();
        var request = httpGet(uri);

        try (var response = executeWithRetry(request, retryTimeout)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token look up returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), TokenLookUpResponse.class);
                return Result.success(payload);
            } else {
                return Result.failure("Token look up failed with status %d".formatted(response.code()));
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            var errMsg = "Failed to look up token with reason: %s".formatted(e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
        }
    }

    /**
     * Attempts to renew the Vault token with the configured ttl. Note that Vault will not honor the passed
     * ttl (or increment) for periodic tokens. Therefore, the ttl returned by this operation should always be used
     * for further calculations.
     * <p>
     * Will retry in some error cases.
     *
     * @param retryTimeout the retry timeout in seconds. Set this to 0 for no retries.
     * @return the result of the token renewal operation
     */
    public Result<TokenRenewResponse> renewToken(long retryTimeout) {
        var uri = getVaultUrl()
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(TOKEN_RENEW_SELF_PATH))
                .build();
        var requestPayload = TokenRenewRequest.Builder
                .newInstance()
                .increment(configValues.ttl())
                .build();
        var request = httpPost(uri, requestPayload);

        try (var response = executeWithRetry(request, retryTimeout)) {
            if (response.isSuccessful()) {
                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure("Token renew returned empty body");
                }
                var payload = objectMapper.readValue(responseBody.string(), TokenRenewResponse.class);
                if (!payload.getWarnings().isEmpty()) {
                    var warnings = String.join(DELIMITER, payload.getWarnings());
                    monitor.warning("Token renew returned: " + warnings);
                }
                return Result.success(payload);
            } else {
                return Result.failure("Token renew failed with status: %d".formatted(response.code()));
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            var errMsg = "Failed to renew token with reason: %s".formatted(e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
        }
    }

    private void scheduleNextTokenRenewal(long ttl) {
        var renewBuffer = configValues.renewBuffer();
        var delay = ttl - renewBuffer;

        scheduledExecutorService.schedule(() -> {
            var tokenRenewResult = renewToken(renewBuffer);

            if (tokenRenewResult.succeeded()) {
                var tokenRenewResponse = tokenRenewResult.getContent();
                scheduleNextTokenRenewal(tokenRenewResponse.getAuth().getLeaseDuration());
            } else {
                monitor.warning("Scheduled token renewal failed: %s".formatted(tokenRenewResult.getFailureDetail()));
            }
        }, delay, TimeUnit.SECONDS);
    }

    public Result<String> getSecretValue(@NotNull String key) {
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
            var errMsg = "Failed to get secret %s with reason: %s".formatted(key, e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
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
                    return Result.failure("Setting secret %s returned empty body".formatted(key));
                }
                var responseBody = response.body().string();
                var responsePayload =
                        objectMapper.readValue(responseBody, CreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure("Failed to set secret %s with status %d".formatted(key, response.code()));
            }
        } catch (IOException e) {
            var errMsg = "Failed to set secret %s with reason: %s".formatted(key, e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
        }
    }

    public Result<Void> destroySecret(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var request = new Request.Builder().url(requestUri).headers(headers).delete().build();

        try (var response = httpClient.execute(request)) {
            return response.isSuccessful() || response.code() == HTTP_CODE_404
                    ? Result.success()
                    : Result.failure("Failed to destroy secret %s with status %d".formatted(key, response.code()));
        } catch (IOException e) {
            var errMsg = "Failed to destroy secret %s with reason: %s".formatted(key, e.getMessage());
            monitor.warning(errMsg, e);
            return Result.failure(errMsg);
        }
    }

    @NotNull
    private HttpUrl getVaultUrl() {
        var httpUrl = HttpUrl.parse(configValues.url());

        if (httpUrl == null) {
            var errMsg = "Vault url is null";
            monitor.warning(errMsg);
            throw new EdcException(errMsg);
        }

        return httpUrl;
    }

    @NotNull
    private HttpUrl getHealthCheckUrl() {
        var vaultHealthPath = configValues.healthCheckPath();
        var isVaultHealthStandbyOk = configValues.healthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return getVaultUrl()
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

        return getVaultUrl()
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

    /**
     * Executes the specified request synchronously. Failed requests will be retried with an exponential backoff (delay)
     * defined as {@code exponentialBackoff = base^retries}.
     * Given an exponential base of 1.5s the backoff will be 1.5s, 2.25, 3.375s, 5.09s, 7.59s, 11.3s, ...
     * <p>
     * Requests will be retried if
     * <ol>
     *     <li>the response status code is retryable and</li>
     *     <li>execution time (delay) is within the remaining time</li>
     * </ol>
     * <p>
     * This method is not relying on httpClient.execute(request, fallbacks) method since the http client retry policy
     * might not fit the recovery time of the Vault.
     *
     * @param request        the request to execute
     * @param timeoutSeconds timeout which will break the loop ultimately
     * @return result of the http request
     * @throws IOException          httpclient was not able to execute the request
     * @throws ExecutionException   result of a retried http request could not be retrieved
     * @throws InterruptedException execution was interrupted while waiting for the result of a retried http request
     */
    @NotNull
    private Response executeWithRetry(Request request, long timeoutSeconds) throws IOException, ExecutionException, InterruptedException {
        var retryCounter = 0;
        var expBackoffSeconds = 0d;
        Response response;

        do {
            var start = Instant.now();
            var delayMillis = Math.round(expBackoffSeconds * 1000L);
            var future = scheduledExecutorService.schedule(
                    () -> httpClient.execute(request),
                    delayMillis,
                    TimeUnit.MILLISECONDS);
            response = future.get();
            expBackoffSeconds = Math.pow(configValues.retryBackoffBase(), retryCounter++);
            var stop = Instant.now();
            var duration = Duration.between(start, stop);
            timeoutSeconds -= duration.getSeconds();
        } while (!NON_RETRYABLE_STATUS_CODES.contains(response.code()) &&  expBackoffSeconds < timeoutSeconds);

        return response;
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
