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

package org.eclipse.http.client;

import dev.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.spi.http.FallbackFactories.retryWhenStatusIsNot;
import static org.eclipse.edc.spi.http.FallbackFactories.retryWhenStatusIsNotIn;
import static org.eclipse.edc.spi.http.FallbackFactories.retryWhenStatusNot2xxOr4xx;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.matchers.Times.unlimited;
import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.stop.Stop.stopQuietly;
import static org.mockserver.verify.VerificationTimes.exactly;

class EdcHttpClientImplTest {

    private final int port = getFreePort();

    private final TypeManager typeManager = new TypeManager();
    private ClientAndServer server;

    @NotNull
    private static EdcHttpClient clientWith(RetryPolicy<Response> retryPolicy) {
        return new EdcHttpClientImpl(testOkHttpClient(), retryPolicy, mock());
    }

    @BeforeEach
    public void startServer() {
        server = ClientAndServer.startClientAndServer(port);
    }

    @AfterEach
    public void stopServer() {
        stopQuietly(server);
    }

    @Test
    void execute_fallback_shouldMapResultWhenResponseIsSuccessful() {
        var client = clientWith(RetryPolicy.ofDefaults());
        server.when(request(), once()).respond(new HttpResponse().withStatusCode(200).withBody(json(Map.of("message", "data"))));

        var request = new Request.Builder()
                .url("http://localhost:" + port)
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo("data");
    }

    @Test
    void execute_fallback_shouldRetry() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());
        server.when(request(), once()).error(error().withDropConnection(true));
        server.when(request(), once()).respond(new HttpResponse().withStatusCode(200).withBody(json(Map.of("message", "data"))));

        var request = new Request.Builder()
                .url("http://localhost:" + port)
                .build();

        var result = client.execute(request, handleResponse());

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo("data");
        server.verify(request(), exactly(2));
    }

    @Test
    void execute_fallback_shouldFailAfterAttemptsExpired_whenResponseFails() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());
        server.when(request(), unlimited()).error(error().withDropConnection(true));

        var request = new Request.Builder()
                .url("http://localhost:" + port)
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
                .url("http://localhost:" + port)
                .build();

        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(500));

        var result = client.execute(request, List.of(retryWhenStatusNot2xxOr4xx()), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));
        server.verify(request(), exactly(2));
    }

    @Test
    void execute_fallback_shouldRetryIfStatusIsNotAsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + port)
                .build();
        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(200));

        var result = client.execute(request, List.of(retryWhenStatusIsNot(204)), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));
        server.verify(request(), exactly(2));
    }

    @Test
    void execute_fallback_shouldRetryIfStatusIsNotContainedInExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + port)
                .build();
        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(400));

        var result = client.execute(request, List.of(retryWhenStatusIsNotIn(200, 204)), handleResponse());

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages)
                .asList().first().asString()
                .matches(it -> it.startsWith("Server response to"))
                .matches(it -> !it.contains("AuthTest"));

        server.verify(request(), exactly(2));
    }

    @Test
    void execute_fallback_shouldFailAfterAttemptsExpired_whenServerIsDown() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());
        server.stop();

        var request = new Request.Builder()
                .header("Authentication", "AuthTest")
                .url("http://localhost:" + port)
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
                .url("http://localhost:" + port)
                .build();

        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(500));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(request(), exactly(2));
    }

    @Test
    void executeAsync_fallback_shouldRetryIfStatusIs4xx() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + port)
                .build();

        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(500));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(request(), exactly(2));
    }

    @Test
    void executeAsync_fallback_shouldNotRetryIfStatusIsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + port)
                .build();

        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(404));

        var result = client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx())).thenApply(handleResponse());

        assertThat(result).succeedsWithin(5, TimeUnit.SECONDS);
        server.verify(request(), exactly(1));
    }

    @Test
    void executeAsync_fallback_shouldRetryIfStatusIsNotAsExpected() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());

        var request = new Request.Builder()
                .url("http://localhost:" + port)
                .build();
        server.when(request(), unlimited()).respond(new HttpResponse().withStatusCode(200));

        var result = client.executeAsync(request, List.of(retryWhenStatusIsNot(204))).thenApply(handleResponse());

        assertThat(result).failsWithin(5, TimeUnit.SECONDS);
        server.verify(request(), exactly(2));
    }

    @Test
    void executeAsync_fallback_shouldFailAfterAttemptsExpired_whenServerIsDown() {
        var client = clientWith(RetryPolicy.<Response>builder().withMaxAttempts(2).build());
        server.stop();

        var request = new Request.Builder()
                .url("http://localhost:" + port)
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
}
