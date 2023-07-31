/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test base for implementations of {@link DspHttpDispatcherDelegate}. Provides methods that test
 * common behaviour of delegate methods. Each sub-class of this test base should choose the
 * respective methods for the delegate implementation it's testing.
 */
public abstract class DspHttpDispatcherDelegateTestBase<M extends RemoteMessage> {

    protected JsonLdRemoteMessageSerializer serializer = mock(JsonLdRemoteMessageSerializer.class);
    protected ObjectMapper mapper = mock(ObjectMapper.class);
    protected TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);

    /**
     * Returns the delegate to test.
     *
     * @return the delegate
     */
    protected abstract DspHttpDispatcherDelegate<M, ?> delegate();

    /**
     * Checks that a delegate throws an exception when the response body is missing. Only relevant
     * for delegates that process the response body.
     */
    protected void testParseResponse_shouldThrowException_whenResponseBodyNull() {
        var response = dummyResponseBuilder(200).body(null).build();

        assertThatThrownBy(() -> delegate().handleResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    /**
     * Checks that a delegate throws an exception when the response body cannot be read. Only
     * relevant for delegates that process the response body.
     */
    protected void testParseResponse_shouldThrowException_whenReadingResponseBodyFails() throws IOException {
        var responseBody = mock(ResponseBody.class);
        var response = dummyResponseBuilder(200).body(responseBody).build();

        when(responseBody.bytes()).thenReturn("test".getBytes());
        when(mapper.readValue(any(byte[].class), eq(JsonObject.class))).thenThrow(IOException.class);

        assertThatThrownBy(() -> delegate().handleResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    /**
     * Checks that a delegate returns a null function for parsing the response body. Only relevant
     * for delegates that do not process the response body.
     */
    protected void testParseResponse_shouldReturnNullFunction_whenResponseBodyNotProcessed() {
        var response = dummyResponseBuilder(200).body(null).build();

        assertThat(delegate().handleResponse().apply(response)).extracting(StatusResult::getContent).isNull();
    }

    protected String readRequestBody(Request request) throws IOException {
        var buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    @NotNull
    protected Response.Builder dummyResponseBuilder(int code) {
        return new Response.Builder()
                .code(code)
                .message("any")
                .body(ResponseBody.create("", MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build());
    }
}
