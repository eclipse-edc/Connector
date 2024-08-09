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

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.http.spi.FallbackFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.EntryMetadata;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayloadGetVaultEntryData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultClientTest {

    private static final String VAULT_URL = "https://mock.url";
    private static final String SECRET_FOLDER = "/foo";
    private static final String HEALTH_PATH = "sys/health";
    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 4L;
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String KEY = "key";
    private static final String DATA_KEY = "data";
    private static final String RENEWABLE_KEY = "renewable";
    private static final String AUTH_KEY = "auth";
    private static final String LEASE_DURATION_KEY = "lease_duration";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final String INCREMENT_KEY = "increment";
    private static final HashicorpVaultSettings HASHICORP_VAULT_CLIENT_CONFIG_VALUES = HashicorpVaultSettings.Builder.newInstance()
            .url(VAULT_URL)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .token(VAULT_TOKEN)
            .ttl(VAULT_TOKEN_TTL)
            .renewBuffer(RENEW_BUFFER)
            .secretPath(CUSTOM_SECRET_PATH)
            .build();

    private static final HashicorpVaultSettings HASHICORP_VAULT_CLIENT_CONFIG_VALUES_WITH_FOLDER = HashicorpVaultSettings.Builder.newInstance()
            .url(VAULT_URL)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .token(VAULT_TOKEN)
            .ttl(VAULT_TOKEN_TTL)
            .renewBuffer(RENEW_BUFFER)
            .secretPath(CUSTOM_SECRET_PATH)
            .folderPath(SECRET_FOLDER)
            .build();

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultClient vaultClient = new HashicorpVaultClient(
            httpClient,
            OBJECT_MAPPER,
            monitor,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES);

    private final HashicorpVaultClient vaultClientWithFolder = new HashicorpVaultClient(
            httpClient,
            OBJECT_MAPPER,
            monitor,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES_WITH_FOLDER);

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

    @Nested
    class Token {
        @Test
        void lookUpToken_whenApiReturns200_shouldSucceed() throws IOException {
            var body = """
                    {
                        "data": {
                            "renewable": true
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
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenLookUpResult = vaultClient.isTokenRenewable();

            verify(httpClient).execute(any(Request.class), argThat((List<FallbackFactory> factories) -> factories.get(0) instanceof HashicorpVaultClientFallbackFactory));
            assertThat(tokenLookUpResult).isSucceeded().satisfies(isRenewable -> assertThat(isRenewable).isTrue());
        }

        @Test
        void lookUpToken_whenApiReturnsErrorCode_shouldFail() throws IOException {
            var response = new Response.Builder()
                    .code(403)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenLookUpResult = vaultClient.isTokenRenewable();

            assertThat(tokenLookUpResult.failed()).isTrue();
            assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Token look up failed with status %s".formatted(403));
        }

        @Test
        void lookUpToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));

            var tokenLookUpResult = vaultClient.isTokenRenewable();

            assertThat(tokenLookUpResult.failed()).isTrue();
            assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Failed to look up token with reason: foo-bar");
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidTokenLookUpResponseArgumentProvider.class)
        void lookUpToken_withInvalidTokenLookUpResponse_shouldFail(Map<String, Object> tokenLookUpResponse) throws IOException {
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(OBJECT_MAPPER.writeValueAsString(tokenLookUpResponse), MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenLookUpResult = vaultClient.isTokenRenewable();

            assertThat(tokenLookUpResult.failed()).isTrue();
            assertThat(tokenLookUpResult.getFailureDetail()).startsWith("Token look up response could not be parsed: Failed to parse renewable flag");
        }

        @Test
        void renewToken_whenApiReturns200_shouldSucceed() throws IOException {
            var body = """
                    {
                        "auth": {
                            "lease_duration": 1800
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
            var tokenRenewRequest = OBJECT_MAPPER.readValue(buffer.readUtf8(), MAP_TYPE_REFERENCE);
            // given a configured ttl of 5 this should equal "5s"
            assertThat(tokenRenewRequest.get(INCREMENT_KEY)).isEqualTo("%ds".formatted(HASHICORP_VAULT_CLIENT_CONFIG_VALUES.ttl()));
            assertThat(tokenRenewResult).isSucceeded().isEqualTo(1800L);
        }

        @Test
        void renewToken_whenApiReturnsErrorCode_shouldFail() throws IOException {
            var response = new Response.Builder()
                    .code(403)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenRenewResult = vaultClient.renewToken();

            assertThat(tokenRenewResult.failed()).isTrue();
            assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Token renew failed with status: %s".formatted(403));
        }

        @Test
        void renewToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));

            var tokenRenewResult = vaultClient.renewToken();

            assertThat(tokenRenewResult.failed()).isTrue();
            assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Failed to renew token with reason: foo-bar");
            // should be called only once
            verify(httpClient).execute(any(Request.class), anyList());
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidTokenRenewResponseArgumentProvider.class)
        void renewToken_withInvalidTokenRenewResponse_shouldFail(Map<String, Object> tokenRenewResponse) throws IOException {
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create(OBJECT_MAPPER.writeValueAsString(tokenRenewResponse), MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class), anyList())).thenReturn(response);

            var tokenRenewResult = vaultClient.renewToken();

            assertThat(tokenRenewResult.failed()).isTrue();
            assertThat(tokenRenewResult.getFailureDetail()).startsWith("Token renew response could not be parsed: Failed to parse ttl");
        }

        private static class InvalidTokenLookUpResponseArgumentProvider implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(Map.of()),
                        arguments(Map.of(DATA_KEY, Map.of())),
                        arguments(Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, "not a boolean")))
                );
            }
        }

        private static class InvalidTokenRenewResponseArgumentProvider implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(Map.of()),
                        arguments(Map.of(AUTH_KEY, Map.of())),
                        arguments(Map.of(AUTH_KEY, Map.of(LEASE_DURATION_KEY, "not a long")))
                );
            }
        }
    }

    @Nested
    class Secret {
        @Test
        void getSecret_whenApiReturns200_shouldSucceed() throws IOException {
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

            var result = vaultClient.getSecretValue(KEY);

            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("GET") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void getSecret_with_folder_whenApiReturns200_shouldSucceed() throws IOException {
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

            var result = vaultClientWithFolder.getSecretValue(KEY);

            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("GET") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data" + SECRET_FOLDER) &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void setSecret_whenApiReturns200_shouldSucceed() throws IOException {
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

            var result = vaultClient.setSecret(KEY, secretValue);

            assertNotNull(result);
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("POST") &&
                    request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                    request.url().encodedPathSegments().contains(KEY)));
        }

        @Test
        void destroySecret_whenApiReturns200_shouldSucceed() throws IOException {
            var response = new Response.Builder()
                    .code(200)
                    .message("any")
                    .body(ResponseBody.create("", MediaType.get("application/json")))
                    .protocol(Protocol.HTTP_1_1)
                    .request(new Request.Builder().url("http://any").build())
                    .build();
            when(httpClient.execute(any(Request.class))).thenReturn(response);

            var result = vaultClient.destroySecret(KEY);

            assertThat(result).isNotNull();
            assertThat(result.succeeded()).isTrue();
            verify(httpClient).execute(argThat(request -> request.method().equalsIgnoreCase("DELETE") &&
                            request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/metadata")
                    /*request.url().encodedPathSegments().contains(KEY)*/));
        }
    }

}
