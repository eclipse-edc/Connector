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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class HashicorpTokenExchangeProviderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EdcHttpClient httpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock(Monitor.class));

    @TempDir
    private Path tempDir;
    private Path subjectTokenFile;

    @BeforeEach
    void setUp() throws IOException {
        subjectTokenFile = tempDir.resolve("token");
        Files.writeString(subjectTokenFile, "the-subject-token");
    }

    private HashicorpTokenExchangeProvider.Builder providerBuilder() {
        return HashicorpTokenExchangeProvider.Builder.newInstance()
                .tokenExchangeUrl(wireMock.baseUrl())
                .subjectTokenPath(subjectTokenFile.toString())
                .scope("read")
                .audience("edcv")
                .role("participant")
                .resource("participant-1")
                .vaultUrl(wireMock.baseUrl())
                .httpClient(httpClient)
                .objectMapper(objectMapper);
    }

    @Test
    void vaultToken_exchangesThenLogsIn() {
        stubExchange("exchanged-jwt");
        stubLogin("vault-token", 3600);

        var token = providerBuilder().build().vaultToken();

        assertThat(token).isEqualTo("vault-token");
        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
                .withRequestBody(containing("grant_type=urn"))
                .withRequestBody(containing("subject_token=the-subject-token"))
                .withRequestBody(containing("resource=participant-1"))
                .withRequestBody(containing("scope=read"))
                .withRequestBody(containing("audience=edcv")));
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/auth/jwt/login")));
    }

    @Test
    void vaultToken_cachesTokenUntilExpiry() {
        stubExchange("exchanged-jwt");
        stubLogin("vault-token", 3600);

        var provider = providerBuilder().build();
        provider.vaultToken();
        provider.vaultToken();

        wireMock.verify(1, postRequestedFor(urlEqualTo("/token")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/auth/jwt/login")));
    }

    @Test
    void vaultToken_reAuthenticatesWhenLeaseExpired() {
        stubExchange("exchanged-jwt");
        stubLogin("vault-token", 0);

        var provider = providerBuilder().build();
        provider.vaultToken();
        provider.vaultToken();

        wireMock.verify(2, postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void vaultToken_exchangeFails_throws() {
        wireMock.stubFor(post(urlEqualTo("/token")).willReturn(unauthorized()));

        assertThatThrownBy(() -> providerBuilder().build().vaultToken())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to exchange subject token");
    }

    @Test
    void vaultToken_loginFails_throws() {
        stubExchange("exchanged-jwt");
        wireMock.stubFor(post(urlEqualTo("/v1/auth/jwt/login")).willReturn(unauthorized()));

        assertThatThrownBy(() -> providerBuilder().build().vaultToken())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to obtain vault token");
    }

    @Test
    void vaultToken_subjectTokenFileMissing_throws() {
        var provider = providerBuilder().subjectTokenPath(tempDir.resolve("missing").toString()).build();

        assertThatThrownBy(provider::vaultToken)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to read subject token");
    }

    private void stubExchange(String accessToken) {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(okJson("{ \"access_token\": \"%s\" }".formatted(accessToken))));
    }

    private void stubLogin(String clientToken, long leaseDuration) {
        wireMock.stubFor(post(urlEqualTo("/v1/auth/jwt/login"))
                .willReturn(okJson("{ \"auth\": { \"client_token\": \"%s\", \"lease_duration\": %d } }".formatted(clientToken, leaseDuration))));
    }
}
