/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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

import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
public class CallbackHttpClientTest {

    private static final String CALLBACK_PATH = "hooks";

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final TypeManager typeManager = new JacksonTypeManager();
    private final EdcHttpClient httpClient = testHttpClient();
    private final Vault vault = mock();
    private CallbackHttpClient callbackHttpClient;

    @BeforeEach
    void setup() {
        callbackHttpClient = new CallbackHttpClient(httpClient, typeManager.getMapper(), vault);
    }

    @Test
    public void dispatch_shouldCallTheHttpCallback() {
        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();
        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        server.stubFor(post("/" + CALLBACK_PATH)
                .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        assertThatCode(() -> callbackHttpClient.dispatch(callback, event)).doesNotThrowAnyException();
    }

    @Test
    public void dispatch_shouldCallTheHttpCallback_WithAuthHeader() {
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
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        callbackHttpClient.dispatch(callback, event);

        server.verify(1, postRequestedFor(urlEqualTo("/" + CALLBACK_PATH))
                .withHeader(authKey, equalTo(authCodeIdValue)));
    }

    @Test
    public void dispatch_shouldThrowExceptionWhenTheCallbackFails() {
        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri(callbackUrl())
                .build();

        var tpEvent = TransferProcessCompleted.Builder.newInstance().transferProcessId("test").callbackAddresses(List.of(callback)).build();
        var event = EventEnvelope.Builder.newInstance().id("test").at(10).payload(tpEvent).build();

        server.stubFor(post("/" + CALLBACK_PATH)
                .withRequestBody(equalToJson(typeManager.writeValueAsString(event)))
                .willReturn(aResponse().withStatus(400).withBody("{}")));

        assertThatThrownBy(() -> callbackHttpClient.dispatch(callback, event)).isInstanceOf(EdcException.class);
    }

    private String callbackUrl() {
        return String.format("http://localhost:%d/%s", server.getPort(), CALLBACK_PATH);
    }
}
