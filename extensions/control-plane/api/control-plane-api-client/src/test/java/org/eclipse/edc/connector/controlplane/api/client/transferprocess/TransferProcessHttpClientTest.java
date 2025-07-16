/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.client.transferprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.http.client.ControlApiHttpClientImpl;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.net.URI;

import static okhttp3.Protocol.HTTP_1_1;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TransferProcessHttpClientTest {

    private final Interceptor interceptor = mock();
    private final Monitor monitor = mock();
    private final ControlApiHttpClient httpClient = new ControlApiHttpClientImpl(testHttpClient(interceptor), mock());

    private final TransferProcessHttpClient transferProcessHttpClient = new TransferProcessHttpClient(
                httpClient, new ObjectMapper(), monitor);

    @Test
    void complete() throws IOException {
        var req = createFlow().callbackAddress(URI.create("http://localhost:8080/test")).build();
        when(interceptor.intercept(any()))
                .thenAnswer(invocation -> createResponse(204, invocation));

        var result = transferProcessHttpClient.completed(req);

        assertThat(result).isSucceeded();
        verifyNoInteractions(monitor);
    }

    @Test
    void complete_shouldSucceed_withRetry() throws IOException {
        var req = createFlow().callbackAddress(URI.create("http://localhost:8080/test")).build();
        when(interceptor.intercept(any()))
                .thenAnswer(invocation -> createResponse(400, invocation))
                .thenAnswer(invocation -> createResponse(204, invocation));

        var result = transferProcessHttpClient.completed(req);

        assertThat(result).isSucceeded();

        verifyNoInteractions(monitor);
    }

    @Test
    void complete_shouldFail_withMaxRetryExceeded() throws IOException {
        var req = createFlow().callbackAddress(URI.create("http://localhost:8080/test")).build();
        when(interceptor.intercept(any()))
                .thenAnswer(invocation -> createResponse(400, invocation))
                .thenAnswer(invocation -> createResponse(400, invocation))
                .thenAnswer(invocation -> createResponse(400, invocation));

        var result = transferProcessHttpClient.completed(req);

        assertThat(result).isFailed();

        verify(monitor).severe(anyString());
    }

    @Test
    void fail() throws IOException {
        var req = createFlow().callbackAddress(URI.create("http://localhost:8080/test")).build();
        when(interceptor.intercept(any()))
                .thenAnswer(invocation -> createResponse(204, invocation));

        var result = transferProcessHttpClient.failed(req, "failure");

        assertThat(result).isSucceeded();
        verifyNoInteractions(monitor);
    }

    @Nested
    class Provisioned {
        @Test
        void shouldSendRequest() throws IOException {
            var req = dataFlowBuilder().callbackAddress(URI.create("http://localhost:8080/test")).build();
            when(interceptor.intercept(any()))
                    .thenAnswer(invocation -> createResponse(204, invocation));

            var result = transferProcessHttpClient.provisioned(req);

            assertThat(result).isSucceeded();
            verifyNoInteractions(monitor);
        }

        @Test
        void shouldRetrySendRequest_whenFailed() throws IOException {
            var req = dataFlowBuilder().callbackAddress(URI.create("http://localhost:8080/test")).build();
            when(interceptor.intercept(any()))
                    .thenAnswer(invocation -> createResponse(400, invocation))
                    .thenAnswer(invocation -> createResponse(204, invocation));

            var result = transferProcessHttpClient.provisioned(req);

            assertThat(result).isSucceeded();

            verifyNoInteractions(monitor);
        }

        @Test
        void shouldFail_whenMaxRetryExceeded() throws IOException {
            var req = dataFlowBuilder().callbackAddress(URI.create("http://localhost:8080/test")).build();
            when(interceptor.intercept(any()))
                    .thenAnswer(invocation -> createResponse(400, invocation))
                    .thenAnswer(invocation -> createResponse(400, invocation))
                    .thenAnswer(invocation -> createResponse(400, invocation));

            var result = transferProcessHttpClient.provisioned(req);

            assertThat(result).isFailed();

            verify(monitor).severe(anyString());
        }

        private DataFlow.Builder dataFlowBuilder() {
            return DataFlow.Builder.newInstance()
                    .id("1")
                    .destination(DataAddress.Builder.newInstance().type("type").build());
        }
    }

    private DataFlow.Builder createFlow() {
        return DataFlow.Builder.newInstance()
                .id("1")
                .source(DataAddress.Builder.newInstance().type("type").build())
                .destination(DataAddress.Builder.newInstance().type("type").build());
    }

    private Response createResponse(int code, InvocationOnMock invocation) {
        Interceptor.Chain chain = invocation.getArgument(0);
        return new Response.Builder()
                .request(chain.request())
                .protocol(HTTP_1_1).code(code)
                .body(ResponseBody.create("", MediaType.get("application/json"))).message("test")
                .build();
    }
}
