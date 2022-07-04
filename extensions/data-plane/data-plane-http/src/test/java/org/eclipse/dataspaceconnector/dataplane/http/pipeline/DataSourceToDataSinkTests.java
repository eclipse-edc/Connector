/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import dev.failsafe.RetryPolicy;
import io.netty.handler.codec.http.HttpMethod;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceToDataSinkTests {
    private static final String NULL_ENDPOINT = "https://example.com/sink";
    private static final String CONTENT_TYPE = "application/json";

    private ExecutorService executor;
    private Monitor monitor;

    /**
     * Provides most common http error status codes.
     *
     * @return Http Error codes as {@link Stream} of {@link Arguments}.
     */
    private static Stream<Arguments> provideCommonErrorCodes() {
        return Stream.of(
                Arguments.of("MOVED_PERMANENTLY_301", 301),
                Arguments.of("FOUND_302", 302),
                Arguments.of("BAD_REQUEST_400", 400),
                Arguments.of("UNAUTHORIZED_401", 401),
                Arguments.of("NOT_FOUND_404", 404),
                Arguments.of("INTERNAL_SERVER_ERROR_500", 500),
                Arguments.of("BAD_GATEWAY_502", 502)
        );
    }

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void verifySuccessfulTransfer() throws Exception {
        var interceptor = mock(Interceptor.class);
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, getRequest(invocation)));

        var sourceClient = testOkHttpClient().newBuilder()
                .addInterceptor(interceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl(NULL_ENDPOINT)
                        .method(HttpMethod.GET.name())
                        .build())
                .name("test.json")
                .requestId("1")
                .retryPolicy(RetryPolicy.ofDefaults())
                .httpClient(sourceClient)
                .build();

        var sinkClient = testOkHttpClient().newBuilder()
                .addInterceptor(interceptor)
                .build();

        var dataSink = HttpDataSink.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl("https://example.com/sink")
                        .method(HttpMethod.POST.name())
                        .contentType(CONTENT_TYPE)
                        .build())
                .requestId("1")
                .httpClient(sinkClient)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        verify(interceptor, times(2)).intercept(isA(Interceptor.Chain.class));
    }

    /**
     * Verifies an exception thrown by the source endpoint is handled correctly.
     */
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideCommonErrorCodes")
    void verifyFailedTransferBecauseOfClient(String name, int errorCode) throws Exception {

        // simulate source error
        var sourceInterceptor = mock(Interceptor.class);
        when(sourceInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(errorCode, getRequest(invocation)));

        var sourceClient = testOkHttpClient().newBuilder()
                .addInterceptor(sourceInterceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl(NULL_ENDPOINT)
                        .method(HttpMethod.GET.name())
                        .build())
                .name("test.json")
                .requestId("1")
                .retryPolicy(RetryPolicy.ofDefaults())
                .httpClient(sourceClient)
                .build();

        var sinkClient = mock(OkHttpClient.class);

        var dataSink = HttpDataSink.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl("https://example.com/sink")
                        .method(HttpMethod.POST.name())
                        .contentType(CONTENT_TYPE)
                        .build())
                .requestId("1")
                .httpClient(sinkClient)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();

        verify(sourceInterceptor).intercept(isA(Interceptor.Chain.class));
    }

    /**
     * Verifies an exception thrown by the sink endpoint is handled correctly.
     */
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideCommonErrorCodes")
    void verifyFailedTransferBecauseOfProvider(String name, int errorCode) throws Exception {

        // source completes normally
        var sourceInterceptor = mock(Interceptor.class);
        when(sourceInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, getRequest(invocation)));

        var sourceClient = testOkHttpClient().newBuilder()
                .addInterceptor(sourceInterceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl(NULL_ENDPOINT)
                        .method(HttpMethod.GET.name())
                        .build())
                .name("test.json")
                .requestId("1")
                .retryPolicy(RetryPolicy.ofDefaults())
                .httpClient(sourceClient)
                .build();

        // sink endpoint raises an exception
        var sinkInterceptor = mock(Interceptor.class);
        when(sinkInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(errorCode, getRequest(invocation)));


        var sinkClient = testOkHttpClient().newBuilder()
                .addInterceptor(sinkInterceptor)
                .build();

        var dataSink = HttpDataSink.Builder.newInstance()
                .params(HttpRequestParams.Builder.newInstance()
                        .baseUrl(NULL_ENDPOINT)
                        .method(HttpMethod.POST.name())
                        .contentType(CONTENT_TYPE)
                        .build())
                .requestId("1")
                .httpClient(sinkClient)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().failed()).isTrue();

        verify(sourceInterceptor).intercept(isA(Interceptor.Chain.class));
        verify(sinkInterceptor).intercept(isA(Interceptor.Chain.class));
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        monitor = mock(Monitor.class);
    }

    private Response createResponse(int code, Request request) {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .code(code)
                .message("")
                .body(ResponseBody.create("", MediaType.parse("application/json")))
                .build();
    }

    private Request getRequest(InvocationOnMock invocation) {
        return invocation.getArgument(0, Interceptor.Chain.class).request();
    }
}
