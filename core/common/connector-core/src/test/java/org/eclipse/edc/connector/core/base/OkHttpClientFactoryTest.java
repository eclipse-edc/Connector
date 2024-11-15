/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.core.base;

import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OkHttpClientFactoryTest {

    private static final String HTTP_URL = "http://localhost:11111";
    private static final String HTTPS_URL = "https://localhost:11111";
    private final Monitor monitor = mock();
    private final EventListener eventListener = mock();

    @Test
    void shouldPrintLogIfHttpsNotEnforced() {
        var configuration = mock(OkHttpClientConfiguration.class);

        var okHttpClient = OkHttpClientFactory.create(configuration, eventListener, monitor)
                .newBuilder().addInterceptor(dummySuccessfulResponse())
                .build();

        assertThatCode(() -> call(okHttpClient, HTTP_URL)).doesNotThrowAnyException();
        assertThatCode(() -> call(okHttpClient, HTTPS_URL)).doesNotThrowAnyException();
        verify(monitor).info(argThat(messageContains("HTTPS enforcement")));
    }

    @Test
    void shouldEnforceHttpsCalls() {
        var configuration = mock(OkHttpClientConfiguration.class);
        when(configuration.isEnforceHttps()).thenReturn(true);

        var okHttpClient = OkHttpClientFactory.create(configuration, eventListener, monitor)
                .newBuilder().addInterceptor(dummySuccessfulResponse())
                .build();

        assertThatThrownBy(() -> call(okHttpClient, HTTP_URL)).isInstanceOf(EdcException.class);
        assertThatCode(() -> call(okHttpClient, HTTPS_URL)).doesNotThrowAnyException();
        verify(monitor, never()).info(argThat(messageContains("HTTPS enforcement")));
    }

    @Test
    void shouldCreateCustomSocketFactory_whenSendSocketBufferIsSet() {
        var configuration = mock(OkHttpClientConfiguration.class);
        when(configuration.getSendBufferSize()).thenReturn(4096);
        when(configuration.getReceiveBufferSize()).thenReturn(4096);

        var okHttpClient = OkHttpClientFactory.create(configuration, eventListener, monitor)
                .newBuilder()
                .build();

        assertThat(okHttpClient.socketFactory()).isNotNull().satisfies(factory -> {
            try (var socket = factory.createSocket()) {
                assertThat(socket.getSendBufferSize()).isEqualTo(4096);
                assertThat(socket.getReceiveBufferSize()).isEqualTo(4096);
            }
        });
    }

    @NotNull
    private Interceptor dummySuccessfulResponse() {
        return it -> new Response.Builder()
                .code(200)
                .message("any")
                .body(ResponseBody.create("", MediaType.get("text/html")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build())
                .build();
    }

    private void call(OkHttpClient okHttpClient, String url) throws IOException {
        okHttpClient.newCall(new Request.Builder().url(url).build()).execute().close();
    }

    @NotNull
    private ArgumentMatcher<String> messageContains(String string) {
        return message -> message.contains(string);
    }

}
