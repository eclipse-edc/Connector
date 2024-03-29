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
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.security.Vault;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

@ComponentTest
public class GenericHttpRemoteDispatcherWrapperTest {

    private static final int CALLBACK_PORT = getFreePort();
    private static final String CALLBACK_PATH = "hooks";
    private static ClientAndServer receiverEndpointServer;
    private final TypeManager typeManager = new TypeManager();
    private final EdcHttpClient httpClient = spy(testHttpClient());
    private final Vault vault = mock();
    private final GenericHttpRemoteDispatcherImpl dispatcher = new GenericHttpRemoteDispatcherImpl(httpClient);

    @BeforeEach
    void setup() {
        receiverEndpointServer = startClientAndServer(CALLBACK_PORT);
        dispatcher.registerDelegate(new CallbackEventRemoteMessageDispatcher(typeManager.getMapper(), vault));
    }

    @AfterEach
    void tearDown() {
        stopQuietly(receiverEndpointServer);
    }

    @Test
    public void send_shouldCallTheHttpCallback() throws IOException {
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

        var future = dispatcher.dispatch(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP));

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS);
        verify(httpClient, atMostOnce()).execute(any());
    }

    @Test
    public void send_shouldCallTheHttpCallback_WithAuthHeader() throws IOException {
        var authKey = "authHeader";
        var authCodeId = "authCodeId";
        var authCodeIdValue = "authCodeIdValue";

        when(vault.resolveSecret(authCodeId)).thenReturn(authCodeIdValue);

        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .authKey(authKey)
                .authCodeId(authCodeId)
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();

        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        var request = request().withPath("/" + CALLBACK_PATH)
                .withMethod(HttpMethod.POST.name())
                .withHeader(authKey, authCodeIdValue)
                .withBody(typeManager.writeValueAsString(event));

        receiverEndpointServer.when(request).respond(successfulResponse());

        var future = dispatcher.dispatch(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP));

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS);
        verify(httpClient, atMostOnce()).execute(any());
    }

    @Test
    public void send_shouldThrowExceptionWhenTheCallbackFails() throws IOException {
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

        var future = dispatcher.dispatch(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP));

        assertThat(future).failsWithin(5, TimeUnit.SECONDS).withThrowableThat().havingCause().isInstanceOf(EdcException.class);
        verify(httpClient, atMostOnce()).execute(any());
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
