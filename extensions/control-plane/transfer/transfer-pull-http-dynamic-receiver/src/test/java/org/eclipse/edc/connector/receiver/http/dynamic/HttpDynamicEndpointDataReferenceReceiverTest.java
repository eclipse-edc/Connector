/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.receiver.http.dynamic;


import dev.failsafe.RetryPolicy;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.receiver.http.dynamic.HttpDynamicEndpointDataReferenceReceiver.HTTP_RECEIVER_ENDPOINT;
import static org.eclipse.edc.connector.receiver.http.dynamic.TestFunctions.createTransferProcess;
import static org.eclipse.edc.connector.receiver.http.dynamic.TestFunctions.transferProperties;
import static org.eclipse.edc.connector.receiver.http.dynamic.TestFunctions.transferPropertiesWithAuth;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testOkHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class HttpDynamicEndpointDataReferenceReceiverTest {


    public static final String TRANSFER_ID = "t-id";
    private static final int RECEIVER_ENDPOINT_PORT = getFreePort();
    private static final String RECEIVER_ENDPOINT_PATH = "path";
    private static ClientAndServer receiverEndpointServer;
    private Monitor monitor;
    private HttpDynamicEndpointDataReferenceReceiver receiver;
    private OkHttpClient httpClient;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;


    private TransferProcessStore transferProcessStore;

    @BeforeEach
    void setup() {

        receiverEndpointServer = startClientAndServer(RECEIVER_ENDPOINT_PORT);
        monitor = mock(Monitor.class);
        transferProcessStore = mock(TransferProcessStore.class);
        httpClient = spy(testOkHttpClient());
        typeManager = new TypeManager();
        retryPolicy = RetryPolicy.builder().withMaxRetries(1).build();
        receiver = HttpDynamicEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .retryPolicy(retryPolicy)
                .typeManager(typeManager)
                .transferProcessStore(transferProcessStore)
                .monitor(monitor)
                .build();
    }

    @AfterEach
    public void teardown() {
        stopQuietly(receiverEndpointServer);
    }

    @Test
    public void send_shouldForwardTheEdr_withReceiverEndpoint() throws ExecutionException, InterruptedException {


        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(createTransferProcess(TRANSFER_ID, transferProperties(receiverUrl())));

        var edr = createEndpointDataReferenceBuilder()
                .properties(Map.of(HTTP_RECEIVER_ENDPOINT, receiverUrl()))
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);
        verify(httpClient, atMostOnce()).newCall(any());
    }

    @Test
    public void send_shouldForwardTheEdr_withReceiverEndpointAndHeaders() throws ExecutionException, InterruptedException {
        var authKey = "key";
        var authToken = "token";

        receiver = HttpDynamicEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .retryPolicy(retryPolicy)
                .typeManager(typeManager)
                .transferProcessStore(transferProcessStore)
                .authHeader(authKey, authToken)
                .monitor(monitor)
                .build();


        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(createTransferProcess(TRANSFER_ID, transferPropertiesWithAuth(receiverUrl(), authKey, authToken)));


        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withHeader(authKey, authToken)
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);
        verify(httpClient, atMostOnce()).newCall(any());

    }

    @Test
    public void send_shouldFailForwardTheEdr_withPathNotFound() throws ExecutionException, InterruptedException {
        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(createTransferProcess(TRANSFER_ID, transferProperties(receiverUrl() + "/modified")));

        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());

        assertThat(receiver.send(edr).get()).matches(Result::failed);
        verify(httpClient, atMostOnce()).newCall(any());

    }

    @Test
    public void send_shouldFailForwardTheEdr_processIdNotFound() throws ExecutionException, InterruptedException {
        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(null);

        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());

        assertThat(receiver.send(edr).get()).matches(Result::failed);

    }

    @Test
    public void send_shouldFailForwardTheEdr_transferProcessNotFound() throws ExecutionException, InterruptedException {
        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(null);

        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());


        assertThat(receiver.send(edr).get()).matches(Result::failed);

    }

    @Test
    public void send_shouldNotForwardTheEdr_whenReceiverUrlMissing() throws ExecutionException, InterruptedException {

        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(createTransferProcess(TRANSFER_ID));

        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);

        verify(monitor, atMostOnce()).debug(anyString());
        verify(httpClient, never()).newCall(any());
    }

    @Test
    public void send_shouldForwardTheEdr_whenReceiverUrlMissingAndFallbackConfigured() throws ExecutionException, InterruptedException {

        receiver = HttpDynamicEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .retryPolicy(retryPolicy)
                .typeManager(typeManager)
                .transferProcessStore(transferProcessStore)
                .fallbackEndpoint(receiverUrl())
                .monitor(monitor)
                .build();

        when(transferProcessStore.processIdForDataRequestId(any())).thenReturn(TRANSFER_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(createTransferProcess(TRANSFER_ID));

        var edr = createEndpointDataReferenceBuilder()
                .build();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);

        verify(httpClient, atMostOnce()).newCall(any());

    }

    private EndpointDataReference.Builder createEndpointDataReferenceBuilder() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("some.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString());
    }

    private String receiverUrl() {
        return String.format("http://localhost:%d/%s", receiverEndpointServer.getLocalPort(), RECEIVER_ENDPOINT_PATH);
    }

    private HttpResponse successfulResponse() {
        return response()
                .withStatusCode(HttpStatusCode.OK_200.code())
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.PLAIN_TEXT_UTF_8.toString())
                .withBody("{}");
    }


}
