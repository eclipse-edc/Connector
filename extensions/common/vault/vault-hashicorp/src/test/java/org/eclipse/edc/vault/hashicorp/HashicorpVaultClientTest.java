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
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
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
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.EntryMetadata;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayloadGetVaultEntryData;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HashicorpVaultClientTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final String KEY = "key";
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String HEALTH_PATH = "sys/health";
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VAULT_URL = "https://mock.url";
    private static final HashicorpVaultConfigValues HASHICORP_VAULT_CLIENT_CONFIG_VALUES = HashicorpVaultConfigValues.Builder.newInstance()
            .url(VAULT_URL)
            .secretPath(CUSTOM_SECRET_PATH)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .token(VAULT_TOKEN)
            .timeoutDuration(TIMEOUT_DURATION)
            .build();

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final ScheduledExecutorService scheduledExecutorService = mock();
    private final HashicorpVaultClient vaultClient = new HashicorpVaultClient(
            httpClient,
            OBJECT_MAPPER,
            scheduledExecutorService,
            monitor,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES);

    @BeforeEach
    void beforeEach() {
        reset(monitor);
    }

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

            var healthCheckResponseResult = vaultClient.doHealthCheck();
            // invoke
            var healthCheckResponse = healthCheckResponseResult.getContent();

            // verify
            assertThat(healthCheckResponseResult.succeeded()).isTrue();
            assertNotNull(healthCheckResponse);
            verify(httpClient).execute(
                    argThat(request -> request.method().equalsIgnoreCase("GET") &&
                            request.url().encodedPath().contains(HEALTH_PATH) &&
                            request.url().queryParameter("standbyok").equals("false") &&
                            request.url().queryParameter("perfstandbyok").equals("false")));
            assertEquals(200, healthCheckResponse.getCode());
            assertEquals(
                    HealthCheckResponse.HashicorpVaultHealthResponseCode
                            .INITIALIZED_UNSEALED_AND_ACTIVE,
                    healthCheckResponse.getCodeAsEnum());

            var healthCheckResponsePayload = healthCheckResponse.getPayload();

            assertThat(healthCheckResponsePayload).isNotNull();
            assertThat(healthCheckResponsePayload.isInitialized()).isTrue();
            assertThat(healthCheckResponsePayload.isSealed()).isFalse();
            assertThat(healthCheckResponsePayload.isStandby()).isFalse();
            assertThat(healthCheckResponsePayload.isPerformanceStandby()).isFalse();
            assertThat("mode").isEqualTo(healthCheckResponsePayload.getReplicationPerformanceMode());
            assertThat("mode").isEqualTo(healthCheckResponsePayload.getReplicationDrMode());
            assertThat(100).isEqualTo(healthCheckResponsePayload.getServerTimeUtc());
            assertThat("1.0.0").isEqualTo(healthCheckResponsePayload.getVersion());
            assertThat("id").isEqualTo(healthCheckResponsePayload.getClusterId());
            assertThat("name").isEqualTo(healthCheckResponsePayload.getClusterName());
        }

        @ParameterizedTest
        @ValueSource(ints = {429, 272, 473, 501, 503})
        void doHealthCheck_whenHealthCheckReturnsNot200_shouldSucceedWithEmptyPayload(int code) throws IOException {
            // prepare
            var body = "something non-parseable";
            var response = new Response.Builder()
                    .code(code)
                    .message("any")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class))).thenReturn(response);

            var healthCheckResponseResult = vaultClient.doHealthCheck();
            // invoke
            var healthCheckResponse = healthCheckResponseResult.getContent();

            // verify
            assertThat(healthCheckResponseResult.succeeded()).isTrue();
            assertThat(healthCheckResponse).isNotNull();
            verify(httpClient).execute(
                    argThat(request -> request.method().equalsIgnoreCase("GET") &&
                            request.url().encodedPath().contains(HEALTH_PATH) &&
                            request.url().queryParameter("standbyok").equals("false") &&
                            request.url().queryParameter("perfstandbyok").equals("false")));
            assertThat(code).isEqualTo(healthCheckResponse.getCode());
            assertNotEquals(
                    HealthCheckResponse.HashicorpVaultHealthResponseCode.INITIALIZED_UNSEALED_AND_ACTIVE,
                    healthCheckResponse.getCodeAsEnum());
            var healthCheckResponsePayload = healthCheckResponse.getPayload();
            assertThat(healthCheckResponsePayload).isNull();
        }

        @Test
        void doHealthCheck_whenHealthCheckThrowsIoException_shouldFail() throws IOException {
            // prepare
            when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

            // invoke
            var healthCheckResponseResult = vaultClient.doHealthCheck();

            // verify
            assertThat(healthCheckResponseResult.failed()).isTrue();
            verify(monitor).warning("Failed to perform healthcheck with reason: foo-bar");
            assertThat(healthCheckResponseResult.getFailureMessages()).isEqualTo(List.of("Failed to perform healthcheck"));
        }
    }

    @Nested
    class Token {

        @Nested
        class LookUp {

            @Test
            void lookUpToken_whenApiReturns200_shouldSucceed() throws IOException {
                var body = """
                        {
                        "data": {
                            "renewable": true,
                            "policies": ["root"]
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
                when(httpClient.execute(any(Request.class))).thenReturn(response);

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.succeeded()).isTrue();
                var token = tokenLookUpResult.getContent();
                assertThat(token.getPolicies()).isEqualTo(List.of("root"));
                assertThat(token.isRootToken()).isTrue();
            }

            @Test
            void lookUpToken_whenApiReturns403_shouldFail() throws IOException {
                var response = new Response.Builder()
                        .code(403)
                        .message("any")
                        .body(ResponseBody.create("", MediaType.get("application/json")))
                        .protocol(Protocol.HTTP_1_1)
                        .request(new Request.Builder().url("http://any").build())
                        .build();
                when(httpClient.execute(any(Request.class))).thenReturn(response);

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Token look up failed with status 403"));
            }

            @Test
            void lookUpToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
                when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                verify(monitor).warning("Failed to look up token with reason: foo-bar");
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Token look up failed"));
            }
        }

        @Nested
        class Renew {
            @Test
            void renewToken_whenApiReturns200_shouldSucceed() throws IOException {
                var body = """
                        {
                            "warnings": ["foo-bar", "hello-world"],
                            "auth": {
                                "lease_duration": 100
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
                when(httpClient.execute(any(Request.class))).thenReturn(response);

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.succeeded()).isTrue();
                var token = tokenLookUpResult.getContent();
                assertThat(token.getTimeToLive()).isEqualTo(100);
                verify(monitor).warning("Token renew returned: foo-bar, hello-world");
            }

            @Test
            void renewToken_whenApiReturns403_shouldFail() throws IOException {
                var response = new Response.Builder()
                        .code(403)
                        .message("any")
                        .body(ResponseBody.create("", MediaType.get("application/json")))
                        .protocol(Protocol.HTTP_1_1)
                        .request(new Request.Builder().url("http://any").build())
                        .build();
                when(httpClient.execute(any(Request.class))).thenReturn(response);

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Token renew failed with status 403"));
            }

            @Test
            void renewToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
                when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                verify(monitor).warning("Failed to renew token: foo-bar");
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("Failed to renew token"));
            }

            @Nested
            class Scheduled {

                @BeforeEach
                void beforeEach() {
                    reset(scheduledExecutorService);
                }

                @Test
                void scheduleNextTokenRenewal_whenPreviousTokenRenewalSucceeded_shouldSucceed() throws IOException {
                    var body = """
                            {
                                "warnings": ["foo-bar", "hello-world"],
                                "auth": {
                                    "lease_duration": 100
                                }
                            }
                            """;
                    var response = new Response.Builder()
                            .code(403)
                            .message("any")
                            .body(ResponseBody.create(body, MediaType.get("application/json")))
                            .protocol(Protocol.HTTP_1_1)
                            .request(new Request.Builder().url("http://any").build())
                            .build();
                    when(httpClient.execute(any(Request.class))).thenReturn(response);
                    var vaultClientSpy = spy(vaultClient);
                    var captor = ArgumentCaptor.forClass(Runnable.class);
                    // break second token renewal call
                    doNothing().when(vaultClientSpy).scheduleNextTokenRenewal(100L);

                    vaultClientSpy.scheduleNextTokenRenewal(500L);
                    verify(scheduledExecutorService).schedule(captor.capture(), eq(500L), eq(TimeUnit.SECONDS));
                    var runnable = captor.getValue();
                    // execute runnable inside executor
                    runnable.run();

                    verifyNoMoreInteractions(scheduledExecutorService);
                    verify(vaultClientSpy, atMost(2)).scheduleNextTokenRenewal(anyLong());
                }

                @Test
                void scheduleNextTokenRenewal_whenPreviousTokenRenewalFailed_shouldFail() throws IOException {
                    var vaultClientSpy = spy(vaultClient);
                    var response = new Response.Builder()
                            .code(403)
                            .message("any")
                            .body(ResponseBody.create("", MediaType.get("application/json")))
                            .protocol(Protocol.HTTP_1_1)
                            .request(new Request.Builder().url("http://any").build())
                            .build();
                    when(httpClient.execute(any(Request.class))).thenReturn(response);
                    var captor = ArgumentCaptor.forClass(Runnable.class);

                    vaultClientSpy.scheduleNextTokenRenewal(500L);
                    verify(scheduledExecutorService).schedule(captor.capture(), eq(500L), eq(TimeUnit.SECONDS));
                    var runnable = captor.getValue();
                    // execute runnable inside executor
                    runnable.run();

                    verify(vaultClientSpy, atMostOnce()).scheduleNextTokenRenewal(anyLong());
                    verify(monitor).warning(matches("Scheduled token renewal failed: Token renew failed with status 403"));
                }
            }
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
