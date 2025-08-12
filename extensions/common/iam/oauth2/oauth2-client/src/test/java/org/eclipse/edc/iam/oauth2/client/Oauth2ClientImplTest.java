/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.util.io.Ports.getFreePort;

class Oauth2ClientImplTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final TypeManager typeManager = new JacksonTypeManager();

    private Oauth2ClientImpl client;

    @BeforeEach
    public void setUp() {
        client = new Oauth2ClientImpl(testHttpClient(), typeManager);
    }

    @Test
    void verifyRequestTokenSuccess() {
        var request = createRequest();

        var formParameters = request.getParams().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), MultiValuePattern.of(equalTo(entry.getValue().toString()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token"));
        server.stubFor(post(anyUrl()).withFormParams(formParameters).willReturn(okJson(responseBody)));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
    }

    @Test
    void verifyRequestTokenSuccess_withExpiresIn() {
        var request = createRequest();

        var formParameters = request.getParams().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), MultiValuePattern.of(equalTo(entry.getValue().toString()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", 1800));
        server.stubFor(post(anyUrl()).withFormParams(formParameters).willReturn(okJson(responseBody)));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isEqualTo(1800);
        assertThat(result.getContent().getAdditional()).doesNotContainKeys("token", "expires_in");
    }

    @Test
    void verifyRequestTokenSuccess_withExpiresIn_whenNotNumber() {
        var request = createRequest();

        var formParameters = request.getParams().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), MultiValuePattern.of(equalTo(entry.getValue().toString()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", "wrong"));
        server.stubFor(post(anyUrl()).withFormParams(formParameters).willReturn(okJson(responseBody)));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isNull();
    }

    @Test
    void verifyRequestTokenSuccess_withAdditionalProperties() {
        var request = createRequest();

        var formParameters = request.getParams().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), MultiValuePattern.of(equalTo(entry.getValue().toString()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", 1800, "scope", "test"));
        server.stubFor(post(anyUrl()).withFormParams(formParameters).willReturn(okJson(responseBody)));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isEqualTo(1800);
        assertThat(result.getContent().getAdditional()).containsEntry("scope", "test");
    }

    @Test
    void verifyFailureIfServerCallFails() {
        var request = createRequest();
        server.stubFor(post(anyUrl()).willReturn(badRequest()));

        var result = client.requestToken(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).startsWith("Server response");
    }

    @Test
    void verifyFailureIfServerIsNotReachable() {

        var request = createRequest(getFreePort());

        var result = client.requestToken(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).startsWith("Failed to connect to");
    }

    private Oauth2CredentialsRequest createRequest() {
        return createRequest(server.getPort());
    }

    private Oauth2CredentialsRequest createRequest(int port) {
        return SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url("http://localhost:" + port)
                .clientId("clientId")
                .clientSecret("clientSecret")
                .grantType("client_credentials")
                .build();
    }


}
