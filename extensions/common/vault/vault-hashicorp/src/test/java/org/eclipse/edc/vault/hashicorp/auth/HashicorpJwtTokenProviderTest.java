/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HashicorpJwtTokenProviderTest {
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private final EdcHttpClient httpClient = mock();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void vaultToken_usesProvidedRoleInJwtLoginRequest() throws Exception {
        var accessTokenBody = """
                { "access_token": "jwt-token" }
                """;
        var accessTokenResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(accessTokenBody, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://idp/token").build())
                .build();

        var vaultTokenBody = """
                { "auth": { "client_token": "vault-token" } }
                """;
        var vaultTokenResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(vaultTokenBody, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://vault/v1/auth/jwt/login").build())
                .build();

        when(httpClient.execute(any(Request.class))).thenReturn(accessTokenResponse, vaultTokenResponse);

        var tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("http://vault/token")
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .role("custom-role")
                .build();

        var requestCaptor = ArgumentCaptor.forClass(Request.class);

        var vaultToken = tokenProvider.vaultToken();

        assertThat(vaultToken).isEqualTo("vault-token");

        verify(httpClient, times(2)).execute(requestCaptor.capture());
        var requests = requestCaptor.getAllValues();
        var loginRequest = requests.get(1);

        var copy = loginRequest.newBuilder().build();
        var buffer = new Buffer();
        copy.body().writeTo(buffer);
        var bodyJson = buffer.readUtf8();

        var jsonNode = objectMapper.readTree(bodyJson);
        assertThat(jsonNode.get("role").asText()).isEqualTo("custom-role");
        assertThat(jsonNode.get("jwt").asText()).isEqualTo("jwt-token");
    }

    @Test
    void vaultToken_unsuccessfulVaultResponse_throwsEdcExceptionWithDetails() throws Exception {
        var accessTokenBody = """
                { "access_token": "jwt-token" }
                """;
        var accessTokenResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(accessTokenBody, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://idp/token").build())
                .build();

        var errorBody = "Something went wrong";
        var errorResponse = new Response.Builder()
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create(errorBody, MediaType.get("text/plain")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://vault/v1/auth/jwt/login").build())
                .build();

        when(httpClient.execute(any(Request.class))).thenReturn(accessTokenResponse, errorResponse);

        var tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("http://vault/token")
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .role("custom-role")
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessage("Failed to obtain vault token, Vault responded with code '%s', message: '%s'".formatted(500, errorBody));
    }

    @Test
    void vaultToken_invalidTokenUrlDuringTokenRetrieval_throwsEdcException() {
        var tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("://invalid-url")
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessage("Failed to parse vault url '%s'".formatted("://invalid-url"));

        verifyNoInteractions(httpClient);
    }

    @Test
    void vaultToken_usesDefaultRoleWhenNoneSpecified() throws Exception {
        var accessTokenBody = """
                { "access_token": "jwt-token" }
                """;
        var accessTokenResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(accessTokenBody, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://idp/token").build())
                .build();

        var vaultTokenBody = """
                { "auth": { "client_token": "vault-token" } }
                """;
        var vaultTokenResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(vaultTokenBody, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://vault/v1/auth/jwt/login").build())
                .build();

        when(httpClient.execute(any(Request.class))).thenReturn(accessTokenResponse, vaultTokenResponse);

        var tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("http://vault/token")
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .build();

        var requestCaptor = ArgumentCaptor.forClass(Request.class);

        var vaultToken = tokenProvider.vaultToken();

        assertThat(vaultToken).isEqualTo("vault-token");

        verify(httpClient, times(2)).execute(requestCaptor.capture());
        var requests = requestCaptor.getAllValues();
        var loginRequest = requests.get(1);

        var copy = loginRequest.newBuilder().build();
        var buffer = new Buffer();
        copy.body().writeTo(buffer);
        var bodyJson = buffer.readUtf8();

        var jsonNode = objectMapper.readTree(bodyJson);
        assertThat(jsonNode.get("role").asText()).isEqualTo("participant");
    }

    @Test
    void builder_nullRole_throwsException() {
        var builder = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("http://vault/token")
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .role(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("'role' cannot be 'null' with OAuth2 authentication");
    }
}
