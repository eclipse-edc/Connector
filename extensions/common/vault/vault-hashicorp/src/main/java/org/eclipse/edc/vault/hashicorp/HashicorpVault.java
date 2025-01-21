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
 *       Materna Information & Communications SE - Refactoring
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.eclipse.edc.vault.hashicorp.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_SECRET_METADATA_PATH;
import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_TOKEN_HEADER;

/**
 * Implements a vault backed by Hashicorp Vault.
 */
public class HashicorpVault implements Vault {
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_DATA_ENTRY_NAME = "content";

    private final Monitor monitor;

    private final HashicorpVaultSettings settings;
    private final EdcHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HashicorpVault(@NotNull Monitor monitor, HashicorpVaultSettings settings, EdcHttpClient httpClient, ObjectMapper objectMapper) {
        this.monitor = monitor;
        this.settings = settings;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public @Nullable String resolveSecret(String key) {

        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var request = new Request.Builder()
                .url(requestUri)
                .header(VAULT_TOKEN_HEADER, settings.token())
                .get()
                .build();

        try (var response = httpClient.execute(request)) {

            if (response.isSuccessful()) {

                var responseBody = response.body();
                if (responseBody != null) {
                    // using JsonNode here because it makes traversing down the tree null-safe
                    var payload = objectMapper.readValue(responseBody.string(), JsonNode.class);
                    return payload.path("data").path("data").get(VAULT_DATA_ENTRY_NAME).asText();
                }
                monitor.warning("Secret response body is empty");

            } else {
                if (response.code() == 404) {
                    monitor.warning("Secret not found");
                } else {
                    monitor.warning("Failed to get secret with status %d".formatted(response.code()));
                }
            }
        } catch (IOException e) {
            monitor.warning("Failed to get secret with reason: %s".formatted(e.getMessage()));
        }
        return null;
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {

        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);

        var requestPayload = Map.of("data", Map.of(VAULT_DATA_ENTRY_NAME, value));
        var request = new Request.Builder()
                .url(requestUri)
                .header(VAULT_TOKEN_HEADER, settings.token())
                .post(jsonBody(requestPayload))
                .build();

        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful()) {
                return response.body() == null ? Result.failure("Setting secret returned empty body") : Result.success();
            } else {
                return Result.failure("Failed to set secret with status %d".formatted(response.code()));
            }
        } catch (IOException e) {
            return Result.failure("Failed to set secret with reason: %s".formatted(e.getMessage()));
        }
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var request = new Request.Builder()
                .url(requestUri)
                .header(VAULT_TOKEN_HEADER, settings.token())
                .delete()
                .build();

        try (var response = httpClient.execute(request)) {
            return response.isSuccessful() || response.code() == 404 ? Result.success() : Result.failure("Failed to destroy secret with status %d".formatted(response.code()));
        } catch (IOException e) {
            return Result.failure("Failed to destroy secret with reason: %s".formatted(e.getMessage()));
        }
    }

    private HttpUrl getSecretUrl(String key, String entryType) {
        key = URLEncoder.encode(key, StandardCharsets.UTF_8);

        // restore '/' characters to allow subdirectories
        key = key.replace("%2F", "/");

        var vaultApiPath = settings.secretPath();
        var folderPath = settings.getFolderPath();

        var builder = HttpUrl.parse(settings.url())
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
                .addPathSegment(entryType);

        if (folderPath != null) {
            builder.addPathSegments(PathUtil.trimLeadingOrEndingSlash(folderPath));
        }

        return builder
                .addPathSegments(key)
                .build();
    }

    private RequestBody jsonBody(Object body) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        return RequestBody.create(jsonRepresentation, VaultConstants.MEDIA_TYPE_APPLICATION_JSON);
    }
}
