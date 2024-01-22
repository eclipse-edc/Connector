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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.http.FallbackFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.EntryMetadata;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayloadGetVaultEntryData;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultClientTest {

    private static final String VAULT_URL = "https://mock.url";
    private static final String HEALTH_PATH = "sys/health";
    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 4L;
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String KEY = "key";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HashicorpVaultConfigValues HASHICORP_VAULT_CLIENT_CONFIG_VALUES = HashicorpVaultConfigValues.Builder.newInstance()
            .url(VAULT_URL)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .token(VAULT_TOKEN)
            .ttl(VAULT_TOKEN_TTL)
            .renewBuffer(RENEW_BUFFER)
            .secretPath(CUSTOM_SECRET_PATH)
            .build();

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultClient vaultClient = new HashicorpVaultClient(
            httpClient,
            OBJECT_MAPPER,
            monitor,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES);

    @Nested
    class HealthCheck {
        @Test
        void doHealthCheck_whenHealthCheckReturns200_shouldSucceed() throws IOException {
            // prepare
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

            // invoke
            var healthCheckResponseResult = vaultClient.doHealthCheck();

            // verify
            assertThat(healthCheckResponseResult.succeeded()).isTrue();
            verify(httpClient).execute(
                    argThat(request -> request.method().equalsIgnoreCase("GET") &&
                            request.url().encodedPath().contains(HEALTH_PATH) &&
                            request.url().queryParameter("standbyok").equals("false") &&
                            request.url().queryParameter("perfstandbyok").equals("false")));
        }

        @ParameterizedTest
        @MethodSource("healthCheckErrorResponseProvider")
        void doHealthCheck_whenHealthCheckDoesNotReturn200_shouldFail(HealthCheckTestParameter testParameter) throws IOException {
            // prepare
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

            // invoke
            var healthCheckResult = vaultClient.doHealthCheck();

            // verify
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
            // prepare
            when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

            // invoke
            var healthCheckResult = vaultClient.doHealthCheck();

            // verify
            assertThat(healthCheckResult.failed()).isTrue();
            verify(monitor).warning(eq("Failed to perform healthcheck with reason: foo-bar"), any(IOException.class));
            assertThat(healthCheckResult.getFailureMessages()).isEqualTo(List.of("Failed to perform healthcheck with reason: foo-bar"));
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

    @Nested
    class Token {
        @Test
        void lookUpToken_whenApiReturns200_shouldSucceed() throws IOException {
            var body = """
                    {
                      "request_id": "585caa70-402d-e5c4-0977-5463105ba9be",
                      "lease_id": "2346aa70-402d-e114-0977-546fad339fbe",
                      "lease_duration": 10000,
                      "renewable": false,
                      "data": {
                        "accessor": "wbXdLDXxtwKZf7dLErtAq5vz",
                        "creation_time": 1704989507,
                        "creation_ttl": 2592000,
                        "display_name": "token",
                        "entity_id": "entityId",
                        "expire_time": "2024-02-10T17:11:47.881108+01:00",
                        "explicit_max_ttl": 2592000,
                        "id": "hvs.CAESIDb3_aAS0fYUVzoLKzHDX3-GWOE6Gy5jMAvWRsfjJhVKGh4KHGh2cy5Oc0lNQWdjUVNjdnhNeWJhUk1zbDM2Wks",
                        "issue_time": "2024-01-11T17:11:47.881135+01:00",
                        "meta": null,
                        "num_uses": 5,
                        "orphan": false,
                        "path": "auth/token/create",
                        "period": "1h",
                        "policies": [
                          "default",
                          "root"
                        ],
                        "renewable": true,
                        "ttl": 2589147,
                        "type": "service"
                      },
                      "warnings": null
                    }
                    """;
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenLookUpResult = vaultClient.lookUpToken();

            verify(httpClient).execute(any(Request.class), argThat((List<FallbackFactory> ids) -> ids.get(0) instanceof HashicorpVaultClientFallbackFactory));
            AbstractResultAssert.assertThat(tokenLookUpResult).isSucceeded().satisfies(tokenLookUpResponse -> {
                assertThat(tokenLookUpResponse.getRequestId()).isEqualTo("585caa70-402d-e5c4-0977-5463105ba9be");
                assertThat(tokenLookUpResponse.getLeaseId()).isEqualTo("2346aa70-402d-e114-0977-546fad339fbe");
                assertThat(tokenLookUpResponse.getLeaseDuration()).isEqualTo(10000);
                assertThat(tokenLookUpResponse.isRenewable()).isFalse();
                assertThat(tokenLookUpResponse.getWarnings()).isEmpty();
                assertThat(tokenLookUpResponse.getData()).satisfies(tokenLookUpData -> {
                    assertThat(tokenLookUpData.getAccessor()).isEqualTo("wbXdLDXxtwKZf7dLErtAq5vz");
                    assertThat(tokenLookUpData.getCreationTime()).isEqualTo(1704989507L);
                    assertThat(tokenLookUpData.getCreationTtl()).isEqualTo(2592000L);
                    assertThat(tokenLookUpData.getDisplayName()).isEqualTo("token");
                    assertThat(tokenLookUpData.getEntityId()).isEqualTo("entityId");
                    assertThat(tokenLookUpData.getExpireTime()).isEqualTo("2024-02-10T17:11:47.881108+01:00");
                    assertThat(tokenLookUpData.getId()).isEqualTo("hvs.CAESIDb3_aAS0fYUVzoLKzHDX3-GWOE6Gy5jMAvWRsfjJhVKGh4KHGh2cy5Oc0lNQWdjUVNjdnhNeWJhUk1zbDM2Wks");
                    assertThat(tokenLookUpData.getIssueTime()).isEqualTo("2024-01-11T17:11:47.881135+01:00");
                    assertThat(tokenLookUpData.getMeta()).isEmpty();
                    assertThat(tokenLookUpData.getNumUses()).isEqualTo(5);
                    assertThat(tokenLookUpData.isOrphan()).isFalse();
                    assertThat(tokenLookUpData.getExplicitMaxTtl()).isEqualTo(2592000L);
                    assertThat(tokenLookUpData.getPath()).isEqualTo("auth/token/create");
                    assertThat(tokenLookUpData.getPeriod()).isEqualTo("1h");
                    assertThat(tokenLookUpData.getPolicies()).isEqualTo(List.of("default", "root"));
                    assertThat(tokenLookUpData.isRenewable()).isTrue();
                    assertThat(tokenLookUpData.getTtl()).isEqualTo(2589147L);
                    assertThat(tokenLookUpData.getType()).isEqualTo("service");
                });
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 403, 404, 405, 412, 429, 472, 473, 500, 501, 502, 503})
        void lookUpToken_whenApiReturnsErrorCode_shouldFail(int code) throws IOException {
            var response = new Response.Builder()
                    .code(code)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenLookUpResult = vaultClient.lookUpToken();

            assertThat(tokenLookUpResult.failed()).isTrue();
            assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Token look up failed with status %s".formatted(code));
        }

        @Test
        void lookUpToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));

            var tokenLookUpResult = vaultClient.lookUpToken();

            assertThat(tokenLookUpResult.failed()).isTrue();
            verify(monitor).warning(eq("Failed to look up token with reason: foo-bar"), any(Exception.class));
            assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Failed to look up token with reason: foo-bar"));
        }

        @Test
        void renewToken_whenApiReturns200_shouldSucceed() throws IOException {
            var body = """
                    {
                      "request_id": "08c877ef-bdcd-f4f9-0854-2471e55f064b",
                      "lease_id": "2346aa70-402d-e114-0977-546fad339fbe",
                      "lease_duration": 10000,
                      "renewable": false,
                      "data": null,
                      "warnings": [
                        "foo-bar",
                        "hello-world"
                      ],
                      "auth": {
                        "client_token": "hvs.CAESIFGppynzyYl-21YK6OjpbWhmnBgI5upF9Fnx5XhABeR_Gh4KHGh2cy5LUUxiRWlPSk5ZeEpuaDU1OHpjWEhqVWE",
                        "accessor": "87eM6pGwBm4e6h0e16r0AtJo",
                        "policies": [
                          "default",
                          "root"
                        ],
                        "token_policies": [
                          "default",
                          "root"
                        ],
                        "identity_policies": null,
                        "metadata": null,
                        "orphan": false,
                        "entity_id": "entityId",
                        "lease_duration": 1800,
                        "renewable": true,
                        "mfa_requirement": null
                      }
                    }
                    """;
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            var requestCaptor = ArgumentCaptor.forClass(Request.class);
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenRenewResult = vaultClient.renewToken();

            verify(httpClient).execute(requestCaptor.capture(), argThat((List<FallbackFactory> ids) -> ids.get(0) instanceof HashicorpVaultClientFallbackFactory));
            var request = requestCaptor.getValue();
            var copy = Objects.requireNonNull(request.newBuilder().build());
            var buffer = new Buffer();
            Objects.requireNonNull(copy.body()).writeTo(buffer);
            var tokenRenewRequest = OBJECT_MAPPER.readValue(buffer.readUtf8(), TokenRenewRequest.class);
            // given a configured ttl of 5 this should equal "5s"
            assertThat(tokenRenewRequest.getIncrement()).isEqualTo("%ds".formatted(HASHICORP_VAULT_CLIENT_CONFIG_VALUES.ttl()));
            AbstractResultAssert.assertThat(tokenRenewResult).isSucceeded().satisfies(tokenRenewResponse -> {
                assertThat(tokenRenewResponse.getRequestId()).isEqualTo("08c877ef-bdcd-f4f9-0854-2471e55f064b");
                assertThat(tokenRenewResponse.getLeaseId()).isEqualTo("2346aa70-402d-e114-0977-546fad339fbe");
                assertThat(tokenRenewResponse.getLeaseDuration()).isEqualTo(10000);
                assertThat(tokenRenewResponse.isRenewable()).isFalse();
                assertThat(tokenRenewResponse.getData()).isNull();
                assertThat(tokenRenewResponse.getWarnings()).isEqualTo(List.of("foo-bar", "hello-world"));
                assertThat(tokenRenewResponse.getAuth()).satisfies(tokenRenewAuth -> {
                    assertThat(tokenRenewAuth.getClientToken()).isEqualTo("hvs.CAESIFGppynzyYl-21YK6OjpbWhmnBgI5upF9Fnx5XhABeR_Gh4KHGh2cy5LUUxiRWlPSk5ZeEpuaDU1OHpjWEhqVWE");
                    assertThat(tokenRenewAuth.getAccessor()).isEqualTo("87eM6pGwBm4e6h0e16r0AtJo");
                    assertThat(tokenRenewAuth.getPolicies()).isEqualTo(List.of("default", "root"));
                    assertThat(tokenRenewAuth.getTokenPolicies()).isEqualTo(List.of("default", "root"));
                    assertThat(tokenRenewAuth.getIdentityPolicies()).isEmpty();
                    assertThat(tokenRenewAuth.getMetadata()).isEmpty();
                    assertThat(tokenRenewAuth.isOrphan()).isFalse();
                    assertThat(tokenRenewAuth.getEntityId()).isEqualTo("entityId");
                    assertThat(tokenRenewAuth.getLeaseDuration()).isEqualTo(1800L);
                    assertThat(tokenRenewAuth.isRenewable()).isTrue();
                    assertThat(tokenRenewAuth.getMfaRequirement()).isNull();
                });
            });
            verify(monitor).warning("Token renew returned: foo-bar, hello-world");
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 403, 404, 405, 412, 429, 472, 473, 500, 501, 502, 503})
        void renewToken_whenApiReturnsErrorCode_shouldFail(int code) throws IOException {
            var response = new Response.Builder()
                    .code(code)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenRenewResult = vaultClient.renewToken();

            assertThat(tokenRenewResult.failed()).isTrue();
            assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Token renew failed with status: %s".formatted(code));
        }

        @Test
        void renewToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));

            var tokenLookUpResult = vaultClient.renewToken();

            assertThat(tokenLookUpResult.failed()).isTrue();
            verify(monitor).warning(eq("Failed to renew token with reason: foo-bar"), any(Exception.class));
            assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Failed to renew token with reason: foo-bar"));
            // should be called only once
            verify(httpClient).execute(any(Request.class), anyList());
        }
    }

    @Nested
    class Secret {
        @Test
        void getSecret_whenApiReturns200_shouldSucceed() throws IOException {
            // prepare
            var ow = new ObjectMapper().writer();
            var data = GetEntryResponsePayloadGetVaultEntryData.Builder.newInstance().data(new HashMap<>(0)).build();
            var body = GetEntryResponsePayload.Builder.newInstance().data(data).build();
            var bodyString = ow.writeValueAsString(body);
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(bodyString, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();

            when(httpClient.execute(any(Request.class))).thenReturn(response);

            // invoke
            var result = vaultClient.getSecretValue(KEY);

            // verify
            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("GET") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void setSecret_whenApiReturns200_shouldSucceed() throws IOException {
            // prepare
            var ow = new ObjectMapper().writer();
            var data = EntryMetadata.Builder.newInstance().build();
            var body = CreateEntryResponsePayload.Builder.newInstance().data(data).build();
            var bodyString = ow.writeValueAsString(body);
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(bodyString, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            var call = mock(Call.class);
            var secretValue = UUID.randomUUID().toString();

            when(httpClient.execute(any(Request.class))).thenReturn(response);
            when(call.execute()).thenReturn(response);

            // invoke
            var result = vaultClient.setSecret(KEY, secretValue);

            // verify
            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("POST") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void destroySecret_whenApiReturns200_shouldSucceed() throws IOException {
            // prepare
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class))).thenReturn(response);

            // invoke
            var result = vaultClient.destroySecret(KEY);

            // verify
            assertThat(result).isNotNull();
            assertThat(result.succeeded()).isTrue();
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("DELETE") &&
                            request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/metadata")
                    /*request.url().encodedPathSegments().contains(KEY)*/));
        }
    }

}
