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

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.model.ParameterBody;
import org.mockserver.model.Parameters;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

class Oauth2ClientImplTest {

    private final int port = getFreePort();
    private final ClientAndServer server = startClientAndServer(port);
    private final TypeManager typeManager = new JacksonTypeManager();

    private Oauth2ClientImpl client;

    @BeforeEach
    public void setUp() {
        client = new Oauth2ClientImpl(testHttpClient(), typeManager);
    }

    @Test
    void verifyRequestTokenSuccess() {
        var request = createRequest();

        var formParameters = new Parameters(
                request.getParams().entrySet().stream()
                        .map(entry -> Parameter.param(entry.getKey(), entry.getValue().toString()))
                        .collect(Collectors.toList())
        );

        var expectedRequest = HttpRequest.request().withBody(new ParameterBody(formParameters));
        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token"));
        server.when(expectedRequest).respond(HttpResponse.response().withBody(responseBody, APPLICATION_JSON));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
    }

    @Test
    void verifyRequestTokenSuccess_withExpiresIn() {
        var request = createRequest();

        var formParameters = new Parameters(
                request.getParams().entrySet().stream()
                        .map(entry -> Parameter.param(entry.getKey(), entry.getValue().toString()))
                        .collect(Collectors.toList())
        );

        var expectedRequest = HttpRequest.request().withBody(new ParameterBody(formParameters));
        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", 1800));
        server.when(expectedRequest).respond(HttpResponse.response().withBody(responseBody, APPLICATION_JSON));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isEqualTo(1800);
        assertThat(result.getContent().getAdditional()).doesNotContainKeys("token", "expires_in");
    }

    @Test
    void verifyRequestTokenSuccess_withExpiresIn_whenNotNumber() {
        var request = createRequest();

        var formParameters = new Parameters(
                request.getParams().entrySet().stream()
                        .map(entry -> Parameter.param(entry.getKey(), entry.getValue().toString()))
                        .collect(Collectors.toList())
        );

        var expectedRequest = HttpRequest.request().withBody(new ParameterBody(formParameters));
        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", "wrong"));
        server.when(expectedRequest).respond(HttpResponse.response().withBody(responseBody, APPLICATION_JSON));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isNull();
    }

    @Test
    void verifyRequestTokenSuccess_withAdditionalProperties() {
        var request = createRequest();

        var formParameters = new Parameters(
                request.getParams().entrySet().stream()
                        .map(entry -> Parameter.param(entry.getKey(), entry.getValue().toString()))
                        .collect(Collectors.toList())
        );

        var expectedRequest = HttpRequest.request().withBody(new ParameterBody(formParameters));
        var responseBody = typeManager.writeValueAsString(Map.of("access_token", "token", "expires_in", 1800, "scope", "test"));
        server.when(expectedRequest).respond(HttpResponse.response().withBody(responseBody, APPLICATION_JSON));

        var result = client.requestToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getToken()).isEqualTo("token");
        assertThat(result.getContent().getExpiresIn()).isEqualTo(1800);
        assertThat(result.getContent().getAdditional()).containsEntry("scope", "test");
    }

    @Test
    void verifyFailureIfServerCallFails() {
        var request = createRequest();
        server.when(HttpRequest.request()).respond(HttpResponse.response().withStatusCode(400));

        var result = client.requestToken(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).startsWith("Server response");
    }

    @Test
    void verifyFailureIfServerIsNotReachable() {
        server.stop();

        var request = createRequest();

        var result = client.requestToken(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).startsWith("Failed to connect to");
    }

    private Oauth2CredentialsRequest createRequest() {
        return SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url("http://localhost:" + port)
                .clientId("clientId")
                .clientSecret("clientSecret")
                .grantType("client_credentials")
                .build();
    }


}
