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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultHealthServiceTest {

    private static final String VAULT_URL = "https://mock.url";
    private static final String HEALTH_PATH = "sys/health";
    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 4L;
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    
    private static final HashicorpVaultConfig HASHICORP_VAULT_CLIENT_CONFIG_VALUES = HashicorpVaultConfig.Builder.newInstance()
            .vaultUrl(VAULT_URL)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .ttl(VAULT_TOKEN_TTL)
            .renewBuffer(RENEW_BUFFER)
            .secretPath(CUSTOM_SECRET_PATH)
            .build();

    private final EdcHttpClient httpClient = mock();
    private final HashicorpVaultTokenProvider tokenProvider = new HashicorpVaultTokenProviderImpl(VAULT_TOKEN);
    private final HashicorpVaultHealthService vaultClient = new HashicorpVaultHealthService(
            httpClient,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES,
            tokenProvider);

    @Nested
    class HealthCheck {
        @Test
        void doHealthCheck_whenHealthCheckReturns200_shouldSucceed() throws IOException {
            var body = """
                    {
                        "initialized": true,
                        "sealed": false,
                        "standby": false,
                        "performance_standby": false,
                        "replication_performance_mode": "mode",
                        "replication_dr_mode": "mode",
                        "server_time_utc": 100,
                        "version": "1.0.0",
                        "cluster_name": "name",
                        "cluster_id": "id"
                    }
                    """;
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class))).thenReturn(response);

            var healthCheckResponseResult = vaultClient.doHealthCheck();

            assertThat(healthCheckResponseResult).isSucceeded();
            verify(httpClient).execute(
                    argThat(request -> request.method().equalsIgnoreCase("GET") &&
                            request.url().encodedPath().contains(HEALTH_PATH) &&
                            request.url().queryParameter("standbyok").equals("false") &&
                            request.url().queryParameter("perfstandbyok").equals("false")));
        }

        @ParameterizedTest
        @MethodSource("healthCheckErrorResponseProvider")
        void doHealthCheck_whenHealthCheckDoesNotReturn200_shouldFail(HealthCheckTestParameter testParameter) throws IOException {
            var body = """
                    {
                        "initialized": false,
                        "sealed": true,
                        "standby": true,
                        "performance_standby": true,
                        "replication_performance_mode": "mode",
                        "replication_dr_mode": "mode",
                        "server_time_utc": 100,
                        "version": "1.0.0",
                        "cluster_name": "name",
                        "cluster_id": "id"
                    }
                    """;
            var response = new Response.Builder()
                    .code(testParameter.code())
                    .message("any")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class))).thenReturn(response);

            var healthCheckResult = vaultClient.doHealthCheck();

            assertThat(healthCheckResult.failed()).isTrue();
            verify(httpClient).execute(
                    argThat(request -> request.method().equalsIgnoreCase("GET") &&
                            request.url().encodedPath().contains(HEALTH_PATH) &&
                            request.url().queryParameter("standbyok").equals("false") &&
                            request.url().queryParameter("perfstandbyok").equals("false")));
            assertThat(healthCheckResult.getFailureDetail()).isEqualTo("Vault is not available. Reason: %s, additional information: %s".formatted(testParameter.errMsg(), body));
        }

        @Test
        void doHealthCheck_whenHealthCheckThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

            var healthCheckResult = vaultClient.doHealthCheck();

            assertThat(healthCheckResult.failed()).isTrue();
            assertThat(healthCheckResult.getFailureDetail()).isEqualTo("Failed to perform healthcheck with reason: foo-bar");
        }

        private static List<HealthCheckTestParameter> healthCheckErrorResponseProvider() {
            return List.of(
                    new HealthCheckTestParameter(429, "Vault is in standby"),
                    new HealthCheckTestParameter(472, "Vault is in recovery mode"),
                    new HealthCheckTestParameter(473, "Vault is in performance standby"),
                    new HealthCheckTestParameter(501, "Vault is not initialized"),
                    new HealthCheckTestParameter(503, "Vault is sealed"),
                    new HealthCheckTestParameter(999, "Vault returned unspecified code 999")
            );
        }

        private record HealthCheckTestParameter(int code, String errMsg) {
        }
    }
}
