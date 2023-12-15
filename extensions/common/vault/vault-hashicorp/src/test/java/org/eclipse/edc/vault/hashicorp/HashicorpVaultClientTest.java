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
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HashicorpVaultClientTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final String KEY = "key";
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String HEALTH_PATH = "sys/health";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VAULT_URL = "https://mock.url";
    private static final HashicorpVaultConfig HASHICORP_VAULT_CLIENT_CONFIG = HashicorpVaultConfig.Builder.newInstance()
            .url(VAULT_URL)
            .secretPath(CUSTOM_SECRET_PATH)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .token(VAULT_TOKEN)
            .timeout(TIMEOUT)
            .build();

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final ScheduledExecutorService scheduledExecutorService = mock();
    private final HashicorpVaultClient vaultClient = new HashicorpVaultClient(
            HASHICORP_VAULT_CLIENT_CONFIG,
            httpClient,
            OBJECT_MAPPER,
            scheduledExecutorService,
            monitor);

    @BeforeEach
    void beforeEach() {
        reset(monitor);
    }
    @Nested
    class HealthCheck {
        @Test
        void returnsSuccessfulResult_whenHealthCheckCouldBePerformed() throws IOException {
            // prepare
            var response = mock(Response.class);
            var body = mock(ResponseBody.class);
            when(httpClient.execute(any(Request.class))).thenReturn(response);
            when(response.code()).thenReturn(200);
            when(response.isSuccessful()).thenReturn(true);
            when(response.body()).thenReturn(body);
            when(body.string())
                    .thenReturn(
                            "{ " +
                                    "\"initialized\": true, " +
                                    "\"sealed\": false," +
                                    "\"standby\": false," +
                                    "\"performance_standby\": false," +
                                    "\"replication_performance_mode\": \"mode\"," +
                                    "\"replication_dr_mode\": \"mode\"," +
                                    "\"server_time_utc\": 100," +
                                    "\"version\": \"1.0.0\"," +
                                    "\"cluster_name\": \"name\"," +
                                    "\"cluster_id\": \"id\" " +
                                    " }");

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

            assertNotNull(healthCheckResponsePayload);
            Assertions.assertTrue(healthCheckResponsePayload.isInitialized());
            Assertions.assertFalse(healthCheckResponsePayload.isSealed());
            Assertions.assertFalse(healthCheckResponsePayload.isStandby());
            Assertions.assertFalse(healthCheckResponsePayload.isPerformanceStandby());
            assertEquals("mode", healthCheckResponsePayload.getReplicationPerformanceMode());
            assertEquals("mode", healthCheckResponsePayload.getReplicationDrMode());
            assertEquals(100, healthCheckResponsePayload.getServerTimeUtc());
            assertEquals("1.0.0", healthCheckResponsePayload.getVersion());
            assertEquals("id", healthCheckResponsePayload.getClusterId());
            assertEquals("name", healthCheckResponsePayload.getClusterName());
        }

        @Test
        void returnsFailedResult_whenIOExceptionIsThrown() throws IOException {
            // prepare
            when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

            // invoke
            var healthCheckResponseResult = vaultClient.doHealthCheck();

            // verify
            assertThat(healthCheckResponseResult.failed()).isTrue();
            assertThat(healthCheckResponseResult.getFailureMessages()).isEqualTo(List.of("foo-bar"));
        }
    }

    @Nested
    class Token {

        @Nested
        class LookUp {

            @Test
            void returnsSuccessfulResult_whenResponseIs200() throws IOException {
                var response = mock(Response.class);
                var body = mock(ResponseBody.class);
                when(httpClient.execute(any(Request.class))).thenReturn(response);
                when(response.code()).thenReturn(200);
                when(response.isSuccessful()).thenReturn(true);
                when(response.body()).thenReturn(body);
                when(body.string())
                        .thenReturn(
                                "{\"data\":" +
                                        "{" +
                                        "\"explicit_max_ttl\": 300," +
                                        "\"renewable\": true," +
                                        "\"period\": 100," +
                                        "\"policies\": [\"root\"]" +
                                        "}" +
                                        "}");

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.succeeded()).isTrue();
                var token = tokenLookUpResult.getContent();
                assertThat(token.getExplicitMaxTimeToLive()).isEqualTo(300L);
                assertThat(token.hasExplicitMaxTimeToLive()).isTrue();
                assertThat(token.getPeriod()).isEqualTo(100L);
                assertThat(token.isPeriodicToken()).isTrue();
                assertThat(token.getPolicies()).isEqualTo(List.of("root"));
                assertThat(token.isRootToken()).isTrue();
            }

            @Test
            void returnsFailedResult_whenResponseIsNot200() throws IOException {
                var response = mock(Response.class);
                when(httpClient.execute(any(Request.class))).thenReturn(response);
                when(response.code()).thenReturn(403);
                when(response.isSuccessful()).thenReturn(false);

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("403"));
            }

            @Test
            void returnsFailedResult_whenIOExceptionIsThrown() throws IOException {
                when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

                var tokenLookUpResult = vaultClient.lookUpToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("foo-bar"));
            }
        }

        @Nested
        class Renew {
            @Test
            void returnsSuccessfulResult_whenResponseIs200() throws IOException {
                var response = mock(Response.class);
                var body = mock(ResponseBody.class);
                when(httpClient.execute(any(Request.class))).thenReturn(response);
                when(response.code()).thenReturn(200);
                when(response.isSuccessful()).thenReturn(true);
                when(response.body()).thenReturn(body);
                when(body.string())
                        .thenReturn(
                                "{" +
                                        "\"warnings\": [\"foo-bar\", \"hello-world\"]," +
                                        "\"auth\":" +
                                        "{" +
                                        "\"lease_duration\": 100" +
                                        "}" +
                                        "}");

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.succeeded()).isTrue();
                var token = tokenLookUpResult.getContent();
                assertThat(token.getTimeToLive()).isEqualTo(100);
                verify(monitor).warning("foo-bar");
                verify(monitor).warning("hello-world");
            }

            @Test
            void returnsFailedResult_whenResponseIsNot200() throws IOException {
                var response = mock(Response.class);
                when(httpClient.execute(any(Request.class))).thenReturn(response);
                when(response.code()).thenReturn(403);
                when(response.isSuccessful()).thenReturn(false);

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("403"));
            }

            @Test
            void returnsFailedResult_whenIOExceptionIsThrown() throws IOException {
                when(httpClient.execute(any(Request.class))).thenThrow(new IOException("foo-bar"));

                var tokenLookUpResult = vaultClient.renewToken();

                assertThat(tokenLookUpResult.failed()).isTrue();
                assertThat(tokenLookUpResult.getFailureMessages()).isEqualTo(List.of("foo-bar"));
            }

            @Nested
            class Scheduled {

                @BeforeEach
                void beforeEach() {
                    reset(scheduledExecutorService);
                }

                @Test
                void schedulesNextTokenRenewal_whenPreviousTokenRenewalWasSuccessful() throws IOException {
                    var vaultClientSpy = spy(vaultClient);
                    var response = mock(Response.class);
                    var body = mock(ResponseBody.class);
                    when(httpClient.execute(any(Request.class))).thenReturn(response);
                    when(response.code()).thenReturn(200);
                    when(response.isSuccessful()).thenReturn(true);
                    when(response.body()).thenReturn(body);
                    when(body.string())
                            .thenReturn(
                                    "{" +
                                            "\"warnings\": [\"foo-bar\", \"hello-world\"]," +
                                            "\"auth\":" +
                                            "{" +
                                            "\"lease_duration\": 100" +
                                            "}" +
                                            "}");
                    var captor = ArgumentCaptor.forClass(Runnable.class);
                    // break second token renewal call
                    doNothing().when(vaultClientSpy).scheduleNextTokenRenewal(100L);

                    vaultClientSpy.scheduleNextTokenRenewal(500L);
                    verify(scheduledExecutorService).schedule(captor.capture(), eq(500L), eq(TimeUnit.SECONDS));
                    var runnable = captor.getValue();
                    // execute runnable inside executor
                    runnable.run();

                    verifyNoMoreInteractions(scheduledExecutorService);
                    verify(vaultClientSpy, atMost(2                            )).scheduleNextTokenRenewal(anyLong());
                    verify(monitor).info(matches("Token was renewed successfully"));
                    verify(monitor).info(matches("Next token renewal scheduled in 00h:08m:20s"));
                }

                @Test
                void doesNotScheduleNextTokenRenewal_whenPreviousTokenRenewalFailed() throws IOException {
                    var vaultClientSpy = spy(vaultClient);
                    var response = mock(Response.class);
                    when(httpClient.execute(any(Request.class))).thenReturn(response);
                    when(response.code()).thenReturn(403);
                    var captor = ArgumentCaptor.forClass(Runnable.class);

                    vaultClientSpy.scheduleNextTokenRenewal(500L);
                    verify(scheduledExecutorService).schedule(captor.capture(), eq(500L), eq(TimeUnit.SECONDS));
                    var runnable = captor.getValue();
                    // execute runnable inside executor
                    runnable.run();

                    verify(vaultClientSpy, atMostOnce()).scheduleNextTokenRenewal(anyLong());
                    verify(monitor).warning(matches("Failed to renew token: 403"));
                    verify(monitor).info(matches("Next token renewal scheduled in 00h:08m:20s"));
                }
            }
        }
    }

    @Nested
    class Secret {
        @Test
        void getSecretValue() throws IOException {
            // prepare
            var response = mock(Response.class);
            var body = mock(ResponseBody.class);

            var payload = new GetEntryResponsePayload();

            when(httpClient.execute(any(Request.class))).thenReturn(response);
            when(response.code()).thenReturn(200);
            when(response.body()).thenReturn(body);
            when(body.string()).thenReturn(payload.toString());

            // invoke
            var result = vaultClient.getSecretValue(KEY);

            // verify
            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("GET") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void setSecretValue() throws IOException {
            // prepare
            var secretValue = UUID.randomUUID().toString();
            var payload = new CreateEntryResponsePayload();
            var call = mock(Call.class);
            var response = mock(Response.class);
            var body = mock(ResponseBody.class);

            when(httpClient.execute(any(Request.class))).thenReturn(response);
            when(call.execute()).thenReturn(response);
            when(response.code()).thenReturn(200);
            when(response.body()).thenReturn(body);
            when(body.string()).thenReturn(payload.toString());

            // invoke
            var result = vaultClient.setSecret(KEY, secretValue);

            // verify
            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("POST") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void destroySecretValue() throws IOException {
            // prepare
            var response = mock(Response.class);
            var body = mock(ResponseBody.class);
            when(httpClient.execute(any(Request.class))).thenReturn(response);
            when(response.isSuccessful()).thenReturn(true);
            when(response.body()).thenReturn(body);

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
