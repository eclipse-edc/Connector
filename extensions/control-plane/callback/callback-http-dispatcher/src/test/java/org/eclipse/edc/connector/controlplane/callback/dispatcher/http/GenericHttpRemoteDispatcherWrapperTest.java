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

package org.eclipse.edc.connector.controlplane.callback.dispatcher.http;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.callback.dispatcher.http.GenericHttpRemoteDispatcherImpl.CALLBACK_EVENT_HTTP;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes", "resource"})
@ComponentTest
public class GenericHttpRemoteDispatcherWrapperTest {

    private static final String CALLBACK_PATH = "hooks";

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final TypeManager typeManager = new JacksonTypeManager();
    private final EdcHttpClient httpClient = spy(testHttpClient());
    private final Vault vault = mock();
    private final GenericHttpRemoteDispatcherImpl dispatcher = new GenericHttpRemoteDispatcherImpl(httpClient);

    @BeforeEach
    void setup() {
        dispatcher.registerDelegate(new CallbackEventRemoteMessageDispatcher(typeManager.getMapper(), vault));
    }

    @Test
    public void send_shouldCallTheHttpCallback() throws IOException {
        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();

        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        server.stubFor(post("/" + CALLBACK_PATH)
                .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{}")));


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


        server.stubFor(post("/" + CALLBACK_PATH)
                .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{}")));

        var future = dispatcher.dispatch(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP));

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS);
        verify(httpClient, atMostOnce()).execute(any());

        server.verify(1, postRequestedFor(urlEqualTo("/" + CALLBACK_PATH))
                .withHeader(authKey, equalTo(authCodeIdValue)));
    }

    @Test
    public void send_shouldThrowExceptionWhenTheCallbackFails() throws IOException {
        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();

        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();


        server.stubFor(post("/" + CALLBACK_PATH)
                .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{}")));
        var future = dispatcher.dispatch(Object.class, new CallbackEventRemoteMessage<>(callback, event, CALLBACK_EVENT_HTTP));

        assertThat(future).failsWithin(5, TimeUnit.SECONDS).withThrowableThat().havingCause().isInstanceOf(EdcException.class);
        verify(httpClient, atMostOnce()).execute(any());
    }

    private String callbackUrl() {
        return String.format("http://localhost:%d/%s", server.getPort(), CALLBACK_PATH);
    }
}
