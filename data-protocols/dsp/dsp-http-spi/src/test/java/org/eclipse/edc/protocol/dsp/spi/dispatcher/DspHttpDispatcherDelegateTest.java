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

package org.eclipse.edc.protocol.dsp.spi.dispatcher;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DspHttpDispatcherDelegateTest {

    private final JsonLdRemoteMessageSerializer serializer = mock();
    private final Function<Response, Object> parser = mock();
    private final TestDspHttpDispatcherDelegate delegate = new TestDspHttpDispatcherDelegate();

    @Test
    void handleResponse_shouldShouldReturnSuccess_whenResponseIsSuccessful() {
        var response = dummyResponse(200);

        var result = delegate.handleResponse().apply(response);

        assertThat(result).isSucceeded();
        verify(parser).apply(response);
    }

    @Test
    void handleResponse_shouldReturnFatalError_whenResponseIsClientError() {
        var responseBody = "{\"any\": \"value\"}";
        var response = dummyResponseBuilder(400).body(ResponseBody.create(responseBody, MediaType.get("application/json"))).build();

        var result = delegate.handleResponse().apply(response);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.status()).isEqualTo(FATAL_ERROR);
            assertThat(failure.getMessages()).containsOnly(responseBody);
        });
        verifyNoInteractions(parser);
    }

    @Test
    void handleResponse_shouldReturnFatalError_whenResponseIsClientErrorAndBodyIsNull() {
        var response = dummyResponseBuilder(400).body(null).build();

        var result = delegate.handleResponse().apply(response);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.status()).isEqualTo(FATAL_ERROR);
            assertThat(failure.getMessages()).allMatch(it -> it.contains("is null"));
        });
        verifyNoInteractions(parser);
    }

    @Test
    void handleResponse_shouldReturnRetryError_whenResponseIsServerError() {
        var response = dummyResponse(500);

        var result = delegate.handleResponse().apply(response);

        assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(ERROR_RETRY);
        verifyNoInteractions(parser);
    }

    private class TestDspHttpDispatcherDelegate extends DspHttpDispatcherDelegate<RemoteMessage, Object> {

        TestDspHttpDispatcherDelegate() {
            super();
        }

        @Override
        public Function<Response, Object> parseResponse() {
            return parser;
        }
    }

    private static Response dummyResponse(int code) {
        return dummyResponseBuilder(code)
                .build();
    }

    @NotNull
    private static Response.Builder dummyResponseBuilder(int code) {
        return new Response.Builder()
                .code(code)
                .message("any")
                .body(ResponseBody.create("", MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build());
    }

}
