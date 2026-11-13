/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.signaling.domain.DataFlowTerminateMessage;
import org.eclipse.edc.signaling.spi.authorization.Header;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPlaneSignalingClientTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final SignalingAuthorizationRegistry authorizationRegistry = mock();
    private final ObjectMapper objectMapper = new JacksonTypeManager().getMapper();

    @Nested
    class SetupAuthorization {

        @Test
        void shouldSendRequestWithoutAuthorizationHeader_whenNoAuthorizationProfiles() {
            server.stubFor(post(anyUrl()).willReturn(noContent()));
            var client = createClient(dataPlane());
            var message = DataFlowTerminateMessage.Builder.newInstance().messageId(UUID.randomUUID().toString()).build();

            var result = client.terminate("flow-id", message);

            assertThat(result.succeeded()).isTrue();
            server.verify(postRequestedFor(urlPathEqualTo("/flow-id/terminate"))
                    .withoutHeader("Authorization"));
        }

        @Test
        void shouldAddAuthorizationHeader_whenAuthorizationProfilePresent() {
            server.stubFor(post(anyUrl()).willReturn(noContent()));
            var profile = new AuthorizationProfile("oauth2", Map.of());
            var authorization = mock(SignalingAuthorization.class);
            when(authorizationRegistry.findByType("oauth2")).thenReturn(authorization);
            when(authorization.evaluate(profile)).thenReturn(Result.success(new Header("Authorization", "Bearer my-token")));
            var message = DataFlowTerminateMessage.Builder.newInstance().messageId(UUID.randomUUID().toString()).build();

            var result = createClient(dataPlane(profile)).terminate("flow-id", message);

            assertThat(result.succeeded()).isTrue();
            server.verify(postRequestedFor(urlPathEqualTo("/flow-id/terminate"))
                    .withHeader("Authorization", containing("Bearer my-token")));
        }

        @Test
        void shouldFail_whenAuthorizationTypeNotSupported() {
            var profile = new AuthorizationProfile("unknown-type", Map.of());
            when(authorizationRegistry.findByType("unknown-type")).thenReturn(null);
            var message = DataFlowTerminateMessage.Builder.newInstance().messageId(UUID.randomUUID().toString()).build();

            var result = createClient(dataPlane(profile)).terminate("flow-id", message);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("unknown-type"));
            server.verify(0, postRequestedFor(anyUrl()));
        }

        @Test
        void shouldFail_whenAuthorizationEvaluateFails() {
            var profile = new AuthorizationProfile("oauth2", Map.of());
            var authorization = mock(SignalingAuthorization.class);
            when(authorizationRegistry.findByType("oauth2")).thenReturn(authorization);
            when(authorization.evaluate(profile)).thenReturn(Result.failure("token endpoint unreachable"));
            var message = DataFlowTerminateMessage.Builder.newInstance().messageId(UUID.randomUUID().toString()).build();

            var result = createClient(dataPlane(profile)).terminate("flow-id", message);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("token endpoint unreachable"));
            server.verify(0, postRequestedFor(anyUrl()));
        }

        @Test
        void shouldFailWithRetry_whenAuthorizationCouldNotBeObtained() {
            var profile = new AuthorizationProfile("oauth2", Map.of());
            var authorization = mock(SignalingAuthorization.class);
            when(authorizationRegistry.findByType("oauth2")).thenReturn(authorization);
            when(authorization.evaluate(profile)).thenReturn(Result.retryableFailure("token endpoint unreachable"));
            var message = DataFlowTerminateMessage.Builder.newInstance().messageId(UUID.randomUUID().toString()).build();

            var result = createClient(dataPlane(profile)).terminate("flow-id", message);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(ERROR_RETRY);
            server.verify(0, postRequestedFor(anyUrl()));
        }

    }

    @Nested
    class HandleResponse {

        @Test
        void shouldFailWithRetry_whenDataPlaneRespondsWithServerError() {
            server.stubFor(post(anyUrl()).willReturn(serverError()));
            var client = createClient(dataPlane());

            var result = client.prepare(DataFlowPrepareMessage.Builder.newInstance().build());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(ERROR_RETRY);
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("500"));
            server.verify(1, postRequestedFor(anyUrl()));
        }

        @Test
        void shouldFailWithoutRetry_whenDataPlaneRejectsTheSignal() {
            server.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(409).withBody("{\"error\":\"Data flow with id flow-id already exists\"}")));
            var client = createClient(dataPlane());

            var result = client.start(DataFlowStartMessage.Builder.newInstance().build());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("409"));
        }

        @Test
        void shouldFailWithRetry_whenDataPlaneIsNotReachable() {
            var client = createClient(DataPlaneInstance.Builder.newInstance().url("http://localhost:" + getFreePort()).build());

            var result = client.start(DataFlowStartMessage.Builder.newInstance().build());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(ERROR_RETRY);
        }

        @Test
        void shouldSucceed_whenDataPlaneRespondsWithValidBody() {
            server.stubFor(post(anyUrl()).willReturn(ok().withBody("{\"state\": \"STARTED\"}")));
            var client = createClient(dataPlane());

            var result = client.prepare(DataFlowPrepareMessage.Builder.newInstance().build());

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent().getState()).isEqualTo("STARTED");
        }

        @Test
        void shouldFail_whenResponseBodyCannotBeParsed() {
            server.stubFor(post(anyUrl()).willReturn(ok().withBody("not-valid-json")));
            var client = createClient(dataPlane());

            var result = client.prepare(DataFlowPrepareMessage.Builder.newInstance().build());

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailureMessages()).first().asString().contains("Cannot read response body");
        }
    }

    private DataPlaneInstance dataPlane(AuthorizationProfile... profiles) {
        var builder = DataPlaneInstance.Builder.newInstance().url("http://localhost:" + server.getPort());
        for (var profile : profiles) {
            builder.authorizationProfile(profile);
        }
        return builder.build();
    }

    private DataPlaneSignalingClient createClient(DataPlaneInstance dataPlane) {
        return new DataPlaneSignalingClient(dataPlane, testHttpClient(), () -> objectMapper, authorizationRegistry);
    }
}
