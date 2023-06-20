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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
     * Checks that a delegate, given a message, builds the expected HTTP request. The message should
     * be serialized and added as the request body. Validates that the delegate sets the expected
     * request path. Relevant for all delegates.
     *
     * @param message the message
     * @param path    the expected path
     */
    protected void testBuildRequest_shouldReturnRequest(M message, String path) throws IOException {
        var serializedBody = "serialized";

        when(serializer.serialize(eq(message))).thenReturn(serializedBody);

        var httpRequest = delegate().buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCounterPartyAddress() + path);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);

        verify(serializer, times(1)).serialize(eq(message));
    }

    /**
     * Checks that a delegate throws an exception if serialization of the message to send fails.
     * Relevant for all delegates.
     *
     * @param message the message
     */
    protected void testBuildRequest_shouldThrowException_whenSerializationFails(M message) {
        when(serializer.serialize(eq(message))).thenThrow(EdcException.class);

        assertThatThrownBy(() -> delegate().buildRequest(message)).isInstanceOf(EdcException.class);
    }

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
