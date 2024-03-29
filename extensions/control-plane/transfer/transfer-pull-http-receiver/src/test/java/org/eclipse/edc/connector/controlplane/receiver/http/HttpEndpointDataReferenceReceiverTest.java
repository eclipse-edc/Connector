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

package org.eclipse.edc.connector.controlplane.receiver.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
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

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class HttpEndpointDataReferenceReceiverTest {

    private static final int RECEIVER_ENDPOINT_PORT = getFreePort();
    private static final String RECEIVER_ENDPOINT_PATH = "path";

    private final EdcHttpClient httpClient = testHttpClient();
    private final TypeManager typeManager = new TypeManager();
    private final Monitor monitor = mock(Monitor.class);
    private ClientAndServer receiverEndpointServer;
    private HttpEndpointDataReferenceReceiver receiver;

    @BeforeEach
    void setup() {
        receiverEndpointServer = startClientAndServer(RECEIVER_ENDPOINT_PORT);
        receiver = receiverBuilder().build();
    }

    @AfterEach
    public void teardown() {
        stopQuietly(receiverEndpointServer);
    }

    @Test
    public void send_shouldForwardTheEdr_withReceiverEndpoint() throws ExecutionException, InterruptedException {
        var edr = createEndpointDataReference();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);
    }

    @Test
    public void send_shouldForwardTheEdr_withReceiverEndpointAndHeaders() throws ExecutionException, InterruptedException {
        var authKey = "key";
        var authToken = "token";
        receiver = receiverBuilder()
                .authHeader(authKey, authToken)
                .build();

        var edr = createEndpointDataReference();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH)
                .withMethod(HttpMethod.POST.name())
                .withHeader(authKey, authToken)
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());
        var result = receiver.send(edr).get();
        assertThat(result).satisfies(Result::success);
    }

    @Test
    public void send_shouldFailForwardTheEdr_withPathNotFound() {
        var edr = createEndpointDataReference();

        var request = request().withPath("/" + RECEIVER_ENDPOINT_PATH + "/another")
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(edr));

        receiverEndpointServer.when(request).respond(successfulResponse());

        assertThrows(EdcException.class, () -> receiver.send(edr));
    }

    private HttpEndpointDataReferenceReceiver.Builder receiverBuilder() {
        return HttpEndpointDataReferenceReceiver.Builder.newInstance()
                .httpClient(httpClient)
                .typeManager(typeManager)
                .monitor(monitor)
                .endpoint(receiverUrl());
    }

    private EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("some.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .contractId("contractId")
                .build();
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
