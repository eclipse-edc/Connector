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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_AUTHORIZED;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpDataSourceTest {

    private final HttpRequestFactory requestFactory = mock();

    @Test
    void verifyCallSuccess() {
        var responseBody = ResponseBody.create("{}", MediaType.parse("application/json"));

        var interceptor = new CustomInterceptor(200, responseBody, "Test message");
        var params = mock(HttpRequestParams.class);
        var request = dummyRequest();
        var source = defaultBuilder(interceptor).params(params).requestFactory(requestFactory).build();
        when(requestFactory.toRequest(any())).thenReturn(request);

        var parts = source.openPartStream().getContent().toList();

        var interceptedRequest = interceptor.getInterceptedRequest();
        assertThat(interceptedRequest).isEqualTo(request);
        assertThat(parts).hasSize(1).first().satisfies(part -> {
            assertThat(part.mediaType()).startsWith("application/json");
            assertThat(part.openStream()).hasContent("{}");
        });

        verify(requestFactory).toRequest(any());
    }

    @ParameterizedTest
    @ArgumentsSource(StreamFailureArguments.class)
    void verifyCallFailed(int code, StreamFailure.Reason reason) {
        var responseBody = ResponseBody.create("Test body", MediaType.parse("text/plain"));
        var interceptor = new CustomInterceptor(code, responseBody, "Test message");
        var source = defaultBuilder(interceptor).params(mock()).requestFactory(requestFactory).build();
        when(requestFactory.toRequest(any())).thenReturn(dummyRequest());

        var result = source.openPartStream();

        assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(reason);
        verify(requestFactory).toRequest(any());
    }

    @Test
    void close_shouldCloseResponseBodyAndStream() throws IOException {
        InputStream stream = mock();
        var responseBody = spy(ResponseBody.create("{}", MediaType.parse("application/json")));
        when(responseBody.byteStream()).thenReturn(stream);
        var interceptor = new CustomInterceptor(200, responseBody, "Test message");
        var source = defaultBuilder(interceptor).params(mock()).requestFactory(requestFactory).build();
        when(requestFactory.toRequest(any())).thenReturn(dummyRequest());

        source.openPartStream();
        source.close();

        verify(responseBody).close();
        verify(stream).close();
    }

    @NotNull
    private Request dummyRequest() {
        return new Request.Builder().url("http://some.test.url/").get().build();
    }

    private static class StreamFailureArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments(400, GENERAL_ERROR),
                    arguments(401, NOT_AUTHORIZED),
                    arguments(403, NOT_AUTHORIZED),
                    arguments(500, GENERAL_ERROR)
            );
        }
    }

    private HttpDataSource.Builder defaultBuilder(Interceptor interceptor) {
        var httpClient = testHttpClient(interceptor);
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .name("test-name")
                .monitor(mock(Monitor.class))
                .requestId(UUID.randomUUID().toString());
    }

    static final class CustomInterceptor implements Interceptor {
        private final List<Request> requests = new ArrayList<>();
        private final int statusCode;
        private final ResponseBody responseBody;
        private final String message;

        CustomInterceptor(int statusCode, ResponseBody responseBody, String message) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.message = message;
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) {
            requests.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1)
                    .code(statusCode)
                    .body(responseBody)
                    .message(message)
                    .build();
        }

        public Request getInterceptedRequest() {
            return requests.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No request intercepted"));
        }
    }
}
