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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferRequestDelegateTest {

    private ObjectMapper mapper = mock(ObjectMapper.class);
    private JsonLdTransformerRegistry registry = mock(JsonLdTransformerRegistry.class);

    private TransferRequestDelegate requestDelegate;

    @BeforeEach
    void setUp() {
        requestDelegate = new TransferRequestDelegate(mapper, registry);
    }

    @Test
    void getMessageType_returnCatalogRequest() {
        assertThat(requestDelegate.getMessageType()).isEqualTo(TransferRequestMessage.class);
    }

    @Test
    void buildRequest_returnRequest() throws IOException {
        var jsonObject = Json.createObjectBuilder()
                .add(DSPACE_SCHEMA + "key", "value")
                .build();
        var requestBody = "request body";
        
        when(registry.transform(any(TransferRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.success(jsonObject));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(requestBody);
        
        var message = getTransferRequestMessage();
        var request = requestDelegate.buildRequest(message);
        
        assertThat(request.url().url()).hasToString(message.getConnectorAddress() + BASE_PATH + TRANSFER_INITIAL_REQUEST);
        assertThat(readRequestBody(request)).isEqualTo(requestBody);
        
        verify(registry, times(1)).transform(any(TransferRequestMessage.class), eq(JsonObject.class));
        verify(mapper, times(1))
                .writeValueAsString(argThat(json -> ((JsonObject) json).get(CONTEXT) != null && ((JsonObject) json).get(DSPACE_PREFIX + ":key") != null));
    }
    
    @Test
    void buildRequest_transformationFails_throwException() {
        when(registry.transform(any(TransferRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));
        
        assertThatThrownBy(() -> requestDelegate.buildRequest(getTransferRequestMessage())).isInstanceOf(EdcException.class);
    }
    
    @Test
    void buildRequest_writingJsonFails_throwException() throws JsonProcessingException {
        when(registry.transform(any(TransferRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenThrow(JsonProcessingException.class);
        
        assertThatThrownBy(() -> requestDelegate.buildRequest(getTransferRequestMessage())).isInstanceOf(EdcException.class);
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
    
        var result = requestDelegate.parseResponse().apply(response);
    
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
    
        assertThatThrownBy(() -> requestDelegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }
    
    @Test
    void parseResponse_readingResponseBodyFails_throwException() throws IOException {
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);
        
        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn("test".getBytes());
        when(mapper.readValue(any(byte[].class), eq(JsonObject.class))).thenThrow(IOException.class);
        
        assertThatThrownBy(() -> requestDelegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }
    
    @Test
    void parseResponse_responseBodyNull_throwException() {
        var response = mock(Response.class);
        
        when(response.body()).thenReturn(null);
        
        assertThatThrownBy(() -> requestDelegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
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
        
        assertThatThrownBy(() -> requestDelegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    private TransferRequestMessage getTransferRequestMessage() {
        return TransferRequestMessage.Builder.newInstance()
                .id("testId")
                .protocol("dataspace-protocol")
                .connectorAddress("http://test-connector-address")
                .contractId("contractId")
                .assetId("assetId")
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("type")
                        .build())
                .connectorId("connectorId")
                .build();
    }
    
    private String readRequestBody(Request request) throws IOException {
        var buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }
    
    private JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add("prefix", "http://schema").build())
                .add("prefix:key", "value")
                .build();
    }
}
