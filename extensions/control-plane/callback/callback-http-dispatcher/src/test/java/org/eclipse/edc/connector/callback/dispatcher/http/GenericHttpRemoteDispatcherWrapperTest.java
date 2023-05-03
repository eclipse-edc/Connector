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

package org.eclipse.edc.connector.callback.dispatcher.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

public class GenericHttpRemoteDispatcherWrapperTest {

    private static final int CALLBACK_PORT = getFreePort();
    private static final String CALLBACK_PATH = "hooks";
    private static ClientAndServer receiverEndpointServer;
    private final TypeManager typeManager = new TypeManager();
    private EdcHttpClient httpClient;

    @BeforeEach
    void setup() {
        receiverEndpointServer = startClientAndServer(CALLBACK_PORT);
        httpClient = spy(testHttpClient());
    }

    @AfterEach
    void tearDown() {

        stopQuietly(receiverEndpointServer);
    }

    @Test
    public void send_shouldCallTheHttpCallback() throws ExecutionException, InterruptedException, IOException {
        var dispatcher = createDispatcher();

        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();

        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        var request = request().withPath("/" + CALLBACK_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(event));

        receiverEndpointServer.when(request).respond(successfulResponse());


        dispatcher.send(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP)).get();

        verify(httpClient, atMostOnce()).execute(any());


    }

    @Test
    public void send_shouldThrowExceptionWhenTheCallbackFails() throws ExecutionException, InterruptedException, IOException {
        var dispatcher = createDispatcher();

        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();

        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        var request = request().withPath("/" + CALLBACK_PATH)
                .withMethod(HttpMethod.POST.name())
                .withBody(typeManager.writeValueAsString(event));

        receiverEndpointServer.when(request).respond(failedResponse());


        assertThatThrownBy(() -> dispatcher.send(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP)).get())
                .cause()
                .isInstanceOf(EdcException.class);

        verify(httpClient, atMostOnce()).execute(any());


    }

    private RemoteMessageDispatcher createDispatcher() {
        var baseDispatcher = new GenericHttpRemoteDispatcherImpl(httpClient);
        baseDispatcher.registerDelegate(new CallbackEventRemoteMessageDispatcher(typeManager.getMapper()));
        return baseDispatcher;
    }

    private HttpResponse successfulResponse() {
        return response()
                .withStatusCode(HttpStatusCode.OK_200.code())
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.PLAIN_TEXT_UTF_8.toString())
                .withBody("{}");
    }

    private HttpResponse failedResponse() {
        return response()
                .withStatusCode(HttpStatusCode.BAD_REQUEST_400.code())
                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.PLAIN_TEXT_UTF_8.toString())
                .withBody("{}");
    }

    private String callbackUrl() {
        return String.format("http://localhost:%d/%s", receiverEndpointServer.getLocalPort(), CALLBACK_PATH);
    }
}
