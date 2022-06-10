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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpMethod;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.Mockito.mock;

class HttpDataSourceTest {
    private static final Faker FAKER = new Faker();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String endpoint;
    private String name;
    private String path;
    private String queryParams;

    private CustomInterceptor interceptor;

    private static String extractRequestBody(Request request) {
        try {
            var sink = Okio.sink(new ByteArrayOutputStream());
            var bufferedSink = Okio.buffer(sink);
            Objects.requireNonNull(request.body()).writeTo(bufferedSink);
            return bufferedSink.getBuffer().readUtf8();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        endpoint = "http://" + FAKER.internet().domainName();
        name = FAKER.lorem().word();
        path = FAKER.lorem().word();
        queryParams = FAKER.lorem().word();
        interceptor = new CustomInterceptor();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sourceWithNameBlankOrNull_shouldIgnore(String p) {
        var source = defaultBuilder()
                .method(HttpMethod.GET.name())
                .path(p)
                .build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url())
                .hasToString(endpoint + "/");
        assertThat(request.method()).isEqualTo(HttpMethod.GET.name());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sourceWithQueryParamsBlankOrNull_shouldIgnore(String qp) {
        var source = defaultBuilder()
                .method(HttpMethod.GET.name())
                .path(path)
                .queryParams(qp)
                .build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(endpoint + "/" + path);
        assertThat(request.method()).isEqualTo(HttpMethod.GET.name());
    }

    @Test
    void sourceWithNameAndQueryParams() {
        var source = defaultBuilder()
                .method(HttpMethod.GET.name())
                .path(path)
                .queryParams(queryParams)
                .build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(String.format("%s/%s?%s", endpoint, path, queryParams));
        assertThat(request.method()).isEqualTo(HttpMethod.GET.name());
    }

    @Test
    void sourceWithBody() throws JsonProcessingException {
        var json = createBody();
        var source = defaultBuilder()
                .method(HttpMethod.POST.name())
                .requestBody(MediaType.get("application/json"), json).build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(endpoint + "/");
        assertThat(request.method()).isEqualTo(HttpMethod.POST.name());
        assertThat(extractRequestBody(request)).isEqualTo(json);
    }

    @Test
    void verifyOpenPartStream() {
        var source = defaultBuilder().method(HttpMethod.GET.name()).build();

        var parts = source.openPartStream().collect(Collectors.toList());

        assertThat(parts).hasSize(1);
        var part = parts.get(0);
        assertThat(part.name()).isEqualTo(name);
        assertThat(part.openStream()).hasContent(interceptor.getJsonResponse());
    }

    private HttpDataSource.Builder defaultBuilder() {
        var monitor = mock(Monitor.class);
        var retryPolicy = new RetryPolicy<>().withMaxAttempts(1);
        var httpClient = testOkHttpClient().newBuilder().addInterceptor(interceptor).build();
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .monitor(monitor)
                .name(name)
                .sourceUrl(endpoint)
                .requestId(UUID.randomUUID().toString())
                .retryPolicy(retryPolicy);
    }


    static final class CustomInterceptor implements Interceptor {
        private final String jsonResponse;

        private final List<Request> requests = new ArrayList<>();

        CustomInterceptor() throws JsonProcessingException {
            jsonResponse = createBody();
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
            requests.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1).code(200)
                    .body(ResponseBody.create(jsonResponse, MediaType.get("application/json"))).message("ok")
                    .build();
        }

        public Request getInterceptedRequest() {
            return requests.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No request intercepted"));
        }

        public String getJsonResponse() {
            return jsonResponse;
        }
    }

    private static String createBody() throws JsonProcessingException {
        return MAPPER.writeValueAsString(Map.of(FAKER.lorem().word(), FAKER.lorem().word()));
    }
}