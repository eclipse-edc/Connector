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
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class HashicorpJwtTokenProviderTest {
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdcHttpClient httpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock(Monitor.class));
    private final HashicorpJwtTokenProvider.Builder providerBuilder = HashicorpJwtTokenProvider.Builder.newInstance()
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)

            .httpClient(httpClient)
            .objectMapper(objectMapper)
            .role("custom-role");

    @BeforeEach
    void setUp() {
    }


    @Test
    void vaultToken_accessTokenFails_throwsEdcException() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Unauthorized")));

        var tokenProvider = providerBuilder
                .tokenUrl(wireMock.baseUrl() + "/token")
                .vaultUrl(wireMock.baseUrl())
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to obtain JWT access token");

        wireMock.verify(1, postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void vaultToken_tokenUrlNotValid_throwsEdcException() {
        var tokenProvider = providerBuilder
                .tokenUrl("://invalid-url")
                .vaultUrl(wireMock.baseUrl())
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to parse vault url");
    }

    @Test
    void vaultToken_vaultUrlNotValid_throwsEdcException() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("""
                        { "access_token": "jwt-token" }
                        """)));
        var tokenProvider = providerBuilder
                .tokenUrl(wireMock.baseUrl() + "/token")
                .vaultUrl("://invalid-url")
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to parse vault url");
    }

    @Test
    void vaultToken_tokenRequestSuccessful() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("""
                        { "access_token": "jwt-token" }
                        """)));

        wireMock.stubFor(post(urlEqualTo("/v1/auth/jwt/login"))
                .willReturn(okJson("""
                        { "auth": { "client_token": "vault-token" } }
                        """)));

        var tokenProvider = providerBuilder
                .tokenUrl(wireMock.baseUrl() + "/token")
                .vaultUrl(wireMock.baseUrl())
                .build();

        var vaultToken = tokenProvider.vaultToken();

        assertThat(vaultToken).isEqualTo("vault-token");

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/auth/jwt/login"))
                .withRequestBody(equalToJson("""
                        {
                            "role": "custom-role",
                            "jwt": "jwt-token"
                        }
                        """)));
    }

    @Test
    void vaultToken_usesDefaultRoleWhenNoneSpecified() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("""
                        { "access_token": "jwt-token" }
                        """)));

        wireMock.stubFor(post(urlEqualTo("/v1/auth/jwt/login"))
                .willReturn(okJson("""
                        { "auth": { "client_token": "vault-token" } }
                        """)));

        var tokenProvider = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl(wireMock.baseUrl() + "/token")
                .vaultUrl(wireMock.baseUrl())
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .build();

        var vaultToken = tokenProvider.vaultToken();

        assertThat(vaultToken).isEqualTo("vault-token");

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/auth/jwt/login"))
                .withRequestBody(equalToJson("""
                        {
                            "role": "participant",
                            "jwt": "jwt-token"
                        }
                        """)));
    }

    @Test
    void vaultToken_tokenRequestFails_throwsEdcException() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("""
                        { "access_token": "jwt-token" }
                        """)));

        wireMock.stubFor(post(urlEqualTo("/v1/auth/jwt/login"))
                .willReturn(unauthorized()));

        var tokenProvider = providerBuilder
                .tokenUrl(wireMock.baseUrl() + "/token")
                .vaultUrl(wireMock.baseUrl())
                .build();

        assertThatThrownBy(tokenProvider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to obtain vault token");

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/auth/jwt/login")));
    }

    @Test
    void builder_nullRole_throwsException() {
        var builder = HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tokenUrl("http://vault/token")
                .vaultUrl(wireMock.baseUrl())
                .httpClient(httpClient)
                .objectMapper(objectMapper)
                .role(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("'role' cannot be 'null' with OAuth2 authentication");
    }
}
