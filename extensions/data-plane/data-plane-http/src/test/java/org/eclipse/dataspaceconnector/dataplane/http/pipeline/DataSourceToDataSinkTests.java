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

import net.jodah.failsafe.RetryPolicy;
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
import org.mockito.invocation.InvocationOnMock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSourceToDataSinkTests {
    private static final String NULL_ENDPOINT = "https://example.com/sink";

    private ExecutorService executor;
    private Monitor monitor;

    /**
     * Verifies a sink is able to pull data from the source without exceptions if both endpoints are functioning.
     */
    @Test
    void verifySuccessfulTransfer() throws Exception {
        var interceptor = mock(Interceptor.class);
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, getRequest(invocation)));

        var sourceClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .sourceUrl(NULL_ENDPOINT)
                .name("test.json")
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .httpClient(sourceClient)
                .monitor(monitor)
                .build();

        var sinkClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        var dataSink = HttpDataSink.Builder.newInstance()
                .endpoint("https://example.com/sink")
                .requestId("1")
                .httpClient(sinkClient)
                .executorService(executor)
                .monitor(monitor)
                .build();

        assertThat(dataSink.transfer(dataSource).get().succeeded()).isTrue();

        verify(interceptor, times(2)).intercept(isA(Interceptor.Chain.class));
    }

    /**
     * Verifies an exception thrown by the source endpoint is handled correctly.
     */
    @Test
    void verifyFailedTransferBecauseOfClient() throws Exception {

        // simulate source error
        var sourceInterceptor = mock(Interceptor.class);
        when(sourceInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, getRequest(invocation)));

        var sourceClient = new OkHttpClient.Builder()
                .addInterceptor(sourceInterceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .sourceUrl(NULL_ENDPOINT)
                .name("test.json")
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .httpClient(sourceClient)
                .monitor(monitor)
                .build();

        var sinkClient = mock(OkHttpClient.class);

        var dataSink = HttpDataSink.Builder.newInstance()
                .endpoint("https://example.com/sink")
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
    @Test
    void verifyFailedTransferBecauseOfProvider() throws Exception {

        // source completes normally
        var sourceInterceptor = mock(Interceptor.class);
        when(sourceInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, getRequest(invocation)));

        var sourceClient = new OkHttpClient.Builder()
                .addInterceptor(sourceInterceptor)
                .build();

        var dataSource = HttpDataSource.Builder.newInstance()
                .sourceUrl(NULL_ENDPOINT)
                .name("test.json")
                .requestId("1")
                .retryPolicy(new RetryPolicy<>())
                .httpClient(sourceClient)
                .monitor(monitor)
                .build();

        // sink endpoint raises an exception
        var sinkInterceptor = mock(Interceptor.class);
        when(sinkInterceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, getRequest(invocation)));

        var sinkClient = new OkHttpClient.Builder()
                .addInterceptor(sinkInterceptor)
                .build();

        var dataSink = HttpDataSink.Builder.newInstance()
                .endpoint(NULL_ENDPOINT)
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
