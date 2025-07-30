/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.http.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNot;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;

class EdcHttpClientImplTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final TypeManager typeManager = new JacksonTypeManager();

    @NotNull
    private static EdcHttpClient clientWith(RetryPolicy<Response> retryPolicy) {
        return new EdcHttpClientImpl(testOkHttpClient(), retryPolicy, mock());
    }

    public static OkHttpClient testOkHttpClient(Interceptor... interceptors) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES);

        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }

        return builder.build();
    }

    @Test
    void execute_fallback_shouldMapResultWhenResponseIsSuccessful() {
        var client = clientWith(RetryPolicy.ofDefaults());

        server.stubFor(get(anyUrl()).willReturn(okJson("{\"message\": \"data\"}")));

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo("data");
    }

    @Test
    void execute_fallback_shouldRetry() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        server.stubFor(get(anyUrl()).willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willSetStateTo("First Attempt"));

        server.stubFor(get(anyUrl()).willReturn(okJson("{\"message\": \"data\"}"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Attempt")
                .willSetStateTo("Second Attempt"));

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo("data");
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void execute_fallback_shouldFailAfterAttemptsExpired_whenResponseFails() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());
        server.stubFor(get(anyUrl()).willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages).asList()
                .first().asString().matches(it -> it.startsWith("unexpected end of stream on"));
    }

    @Test
    void execute_fallback_shouldRetryIfStatusIsNot2xxOr4xx() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + server.getPort())
                .build();

        server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        var result = client.execute(request, List.of(retryWhenStatusNot2xxOr4xx()), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void execute_fallback_shouldRetryIfStatusIsNotAsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + server.getPort())
                .build();

        server.stubFor(get(anyUrl()).willReturn(ok()));

        var result = client.execute(request, List.of(retryWhenStatusIsNot(204)), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void execute_fallback_shouldRetryIfStatusIsNotContainedInExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + server.getPort())
                .build();
        server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(400)));

        var result = client.execute(request, List.of(retryWhenStatusIsNotIn(200, 204)), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));

        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void execute_fallback_shouldFailAfterAttemptsExpired_whenServerIsDown() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + getFreePort())
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages).asList()
                .first().asString()
                .matches(it -> it.startsWith("Failed to connect to"))
                .matches(it -> !it.contains("AuthTest"));
    }

    @Test
    void executeAsync_fallback_shouldRetryIfStatusIsNotSuccessful() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void executeAsync_fallback_shouldRetryIfStatusIs4xx() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();


        server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void executeAsync_fallback_shouldNotRetryIfStatusIsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404)));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).succeedsWithin(5, TimeUnit.SECONDS);
        server.verify(1, getRequestedFor(anyUrl()));
    }

    @Test
    void executeAsync_fallback_shouldRetryIfStatusIsNotAsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + server.getPort())
                .build();

        server.stubFor(get(anyUrl()).willReturn(ok()));

        var result = client.executeAsync(request, List.of(retryWhenStatusIsNot(204))).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(2, getRequestedFor(anyUrl()));
    }

    @Test
    void executeAsync_fallback_shouldFailAfterAttemptsExpired_whenServerIsDown() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + getFreePort())
                .build();

        var result = client.executeAsync(request, emptyList()).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
    }

    @NotNull
    private Function<Response, Result<String>> handleResponse() {
        return r -> {
            try {
                if (r.isSuccessful()) {
                    return Result.success(typeManager.readValue(r.body().string(), Map.class).get("message").toString());
                } else {
                    return Result.success(r.message());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
