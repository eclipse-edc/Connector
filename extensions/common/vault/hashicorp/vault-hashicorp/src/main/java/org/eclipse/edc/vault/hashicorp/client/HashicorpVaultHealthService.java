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
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * This is a client implementation for interacting with Hashicorp Vault.
 * In particular, this performs periodic health checks.
 */
public class HashicorpVaultHealthService {
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";

    private final EdcHttpClient httpClient;
    private final HashicorpVaultSettings settings;
    private final HttpUrl healthCheckUrl;
    private final HashicorpVaultTokenProvider tokenProvider;

    public HashicorpVaultHealthService(@NotNull EdcHttpClient httpClient,
                                       @NotNull HashicorpVaultSettings settings,
                                       @NotNull HashicorpVaultTokenProvider tokenProvider
                                       ) {
        this.httpClient = httpClient;
        this.settings = settings;
        this.tokenProvider = tokenProvider;
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

    @NotNull
    private HttpUrl getHealthCheckUrl() {
        var vaultHealthPath = settings.healthCheckPath();
        var isVaultHealthStandbyOk = settings.healthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return HttpUrl.parse(settings.url())
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultHealthPath))
                .addQueryParameter("standbyok", isVaultHealthStandbyOk ? "true" : "false")
                .addQueryParameter("perfstandbyok", isVaultHealthStandbyOk ? "true" : "false")
                .build();
    }

    @NotNull
    private Request httpGet(HttpUrl requestUri) {
        return new Request.Builder()
                .url(requestUri)
                .headers(getHeaders(tokenProvider.vaultToken()))
                .get()
                .build();
    }

    private Headers getHeaders(String token) {
        var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        headersBuilder.add(VAULT_TOKEN_HEADER, token);
        return headersBuilder.build();
    }
}
