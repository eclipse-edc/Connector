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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

public class HashicorpVaultClient {
    static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String VAULT_API_VERSION = "v1";
    private static final String VAULT_SECRET_PATH = "secret";
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final String CALL_UNSUCCESSFUL_ERROR_TEMPLATE = "[Hashicorp Vault] Call unsuccessful: %s";
    private static final int HTTP_CODE_404 = 404;
    @NotNull
    private final HashicorpVaultConfig hashicorpVaultConfig;
    @NotNull
    private final EdcHttpClient httpClient;
    @NotNull
    private final TypeManager typeManager;

    HashicorpVaultClient(@NotNull HashicorpVaultConfig hashicorpVaultConfig, @NotNull EdcHttpClient httpClient,
                         @NotNull TypeManager typeManager) {
        this.hashicorpVaultConfig = hashicorpVaultConfig;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
    }

    public Result<String> getSecretValue(@NotNull String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestUri).headers(headers).get().build();

        try (var response = httpClient.execute(request)) {

            if (response.isSuccessful()) {
                if (response.code() == HTTP_CODE_404) {
                    return Result.failure(
                            String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Secret not found"));
                }

                var responseBody = response.body();
                if (responseBody == null) {
                    return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Response body empty"));
                }
                var payload = typeManager.readValue(responseBody.string(), GetEntryResponsePayload.class);
                var value = payload.getData().getData().get(VAULT_DATA_ENTRY_NAME);

                return Result.success(value);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }

        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result<CreateEntryResponsePayload> setSecret(
            @NotNull String key, @NotNull String value) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var requestPayload =
                CreateEntryRequestPayload.Builder.newInstance()
                        .data(Collections.singletonMap(VAULT_DATA_ENTRY_NAME, value))
                        .build();
        var request =
                new Request.Builder()
                        .url(requestUri)
                        .headers(headers)
                        .post(createRequestBody(requestPayload))
                        .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload =
                        typeManager.readValue(responseBody, CreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result<Void> destroySecret(@NotNull String key) {
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

    @NotNull
    private Headers getHeaders() {
        var headersBuilder =
                new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        if (hashicorpVaultConfig.getVaultToken() != null) {
            headersBuilder = headersBuilder.add(VAULT_TOKEN_HEADER, hashicorpVaultConfig.getVaultToken());
        }
        return headersBuilder.build();
    }

    private String getBaseUrl() {
        var baseUrl = hashicorpVaultConfig.getVaultUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }

    private String getSecretUrl(String key, String entryType) {
        var encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create(
                        String.format(
                                "%s/%s/%s/%s/%s",
                                getBaseUrl(), VAULT_API_VERSION, VAULT_SECRET_PATH, entryType, encodedKey))
                .toString();
    }

    private RequestBody createRequestBody(Object requestPayload) {
        var jsonRepresentation = typeManager.writeValueAsString(requestPayload);
        return RequestBody.create(jsonRepresentation, MEDIA_TYPE_APPLICATION_JSON);
    }
}
