/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.http.spi.FallbackFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HashicorpVaultTokenRenewServiceTest {
    
    private static final String VAULT_URL = "https://mock.url";
    private static final String HEALTH_PATH = "sys/health";
    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 4L;
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String DATA_KEY = "data";
    private static final String RENEWABLE_KEY = "renewable";
    private static final String AUTH_KEY = "auth";
    private static final String LEASE_DURATION_KEY = "lease_duration";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final String INCREMENT_KEY = "increment";
    
    private static final HashicorpVaultConfig HASHICORP_VAULT_CLIENT_CONFIG_VALUES = HashicorpVaultConfig.Builder.newInstance()
            .vaultUrl(VAULT_URL)
            .healthCheckPath(HEALTH_PATH)
            .healthStandbyOk(false)
            .ttl(VAULT_TOKEN_TTL)
            .renewBuffer(RENEW_BUFFER)
            .secretPath(CUSTOM_SECRET_PATH)
            .build();
    
    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final HashicorpVaultTokenProvider tokenProvider = new HashicorpVaultTokenProviderImpl(VAULT_TOKEN);
    private final HashicorpVaultTokenRenewService vaultClient = new HashicorpVaultTokenRenewService(
            httpClient,
            OBJECT_MAPPER,
            HASHICORP_VAULT_CLIENT_CONFIG_VALUES,
            tokenProvider,
            monitor);
    
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
            
            assertThat(tokenLookUpResult).isFailed();
            assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Token look up failed with status %s".formatted(403));
        }
        
        @Test
        void lookUpToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));
            
            var tokenLookUpResult = vaultClient.isTokenRenewable();
            
            assertThat(tokenLookUpResult).isFailed();
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
            
            assertThat(tokenLookUpResult).isFailed();
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
            assertThat(tokenRenewRequest.get(INCREMENT_KEY)).isEqualTo("%ds".formatted(HASHICORP_VAULT_CLIENT_CONFIG_VALUES.getTtl()));
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
            
            assertThat(tokenRenewResult).isFailed();
            assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Token renew failed with status: %s".formatted(403));
        }
        
        @Test
        void renewToken_whenHttpClientThrowsIoException_shouldFail() throws IOException {
            when(httpClient.execute(any(Request.class), anyList())).thenThrow(new IOException("foo-bar"));
            
            var tokenRenewResult = vaultClient.renewToken();
            
            assertThat(tokenRenewResult).isFailed();
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
            
            assertThat(tokenRenewResult).isFailed();
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
    
}
