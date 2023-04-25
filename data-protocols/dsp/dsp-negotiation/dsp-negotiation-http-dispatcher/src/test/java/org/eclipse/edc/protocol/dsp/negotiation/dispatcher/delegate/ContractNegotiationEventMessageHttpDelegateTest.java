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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractNegotiationEventMessageHttpDelegateTest {
    
    private final JsonLdRemoteMessageSerializer serializer = mock(JsonLdRemoteMessageSerializer.class);

    private ContractNegotiationEventMessageHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ContractNegotiationEventMessageHttpDelegate(serializer);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractNegotiationEventMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        var serializedBody = "message";
    
        when(serializer.serialize(eq(message), any(JsonObject.class))).thenReturn(serializedBody);

        var httpRequest = delegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + EVENT);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);
    
        verify(serializer, times(1)).serialize(eq(message), any(JsonObject.class));
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        var message = message();
    
        when(serializer.serialize(eq(message), any(JsonObject.class))).thenThrow(EdcException.class);
    
        assertThatThrownBy(() -> delegate.buildRequest(message)).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_returnNull() {
        var response = mock(Response.class);

        assertThat(delegate.parseResponse().apply(response)).isNull();
    }

    private ContractNegotiationEventMessage message() {
        var value = "example";
        return ContractNegotiationEventMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress("http://connector")
                .type(ContractNegotiationEventMessage.Type.FINALIZED)
                .build();
    }

    private String readRequestBody(Request request) throws IOException {
        var buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}