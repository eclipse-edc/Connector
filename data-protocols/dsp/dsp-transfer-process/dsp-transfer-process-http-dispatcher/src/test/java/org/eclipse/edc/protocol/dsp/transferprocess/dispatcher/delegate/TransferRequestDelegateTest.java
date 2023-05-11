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

package org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferRequestDelegateTest extends DspHttpDispatcherDelegateTestBase<TransferRequestMessage> {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);

    private TransferRequestDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new TransferRequestDelegate(serializer, mapper, registry, new TitaniumJsonLd(mock(Monitor.class)));
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(TransferRequestMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        testBuildRequest_shouldReturnRequest(message, BASE_PATH + TRANSFER_INITIAL_REQUEST);
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        testBuildRequest_shouldThrowException_whenSerializationFails(message());
    }

    @Test
    void parseResponse_returnTransferProcess() throws IOException {
        var jsonObject = getJsonObject();
        var transferProcess = TransferProcess.Builder.newInstance().id("id").build();
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);
        var bytes = "test".getBytes();

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(bytes);
        when(mapper.readValue(bytes, JsonObject.class)).thenReturn(jsonObject);
        when(registry.transform(any(JsonObject.class), eq(TransferProcess.class))).thenReturn(Result.success(transferProcess));

        var result = delegate.parseResponse().apply(response);

        assertThat(result).isEqualTo(transferProcess);
        verify(mapper, times(1)).readValue(bytes, JsonObject.class);
        verify(registry, times(1)).transform(isA(JsonObject.class), eq(TransferProcess.class));
    }

    @Test
    void parseResponse_transformationFails_throwException() throws IOException {
        var jsonObject = getJsonObject();
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn("test".getBytes());
        when(mapper.readValue(any(byte[].class), eq(JsonObject.class))).thenReturn(jsonObject);
        when(registry.transform(any(JsonObject.class), eq(TransferProcess.class))).thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> delegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_expandingJsonLdFails_throwException() throws IOException {
        // JSON is missing @context -> expanding returns empty JsonArray
        var jsonObject = Json.createObjectBuilder()
                .add("key", "value")
                .build();
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn("test".getBytes());
        when(mapper.readValue(any(byte[].class), eq(JsonObject.class))).thenReturn(jsonObject);

        assertThatThrownBy(() -> delegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_responseBodyNull_throwException() {
        testParseResponse_shouldThrowException_whenResponseBodyNull();
    }

    @Test
    void parseResponse_readingResponseBodyFails_throwException() throws IOException {
        testParseResponse_shouldThrowException_whenReadingResponseBodyFails();
    }

    @Override
    protected DspHttpDispatcherDelegate<TransferRequestMessage, ?> delegate() {
        return delegate;
    }

    private TransferRequestMessage message() {
        return TransferRequestMessage.Builder.newInstance()
                .id("testId")
                .protocol("dataspace-protocol")
                .callbackAddress("http://test-connector-address")
                .counterPartyAddress("http://test-connector-address")
                .contractId("contractId")
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .build();
    }

    private JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add("prefix", "http://schema").build())
                .add("prefix:key", "value")
                .build();
    }
}
