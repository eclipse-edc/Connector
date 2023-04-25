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

package org.eclipse.edc.protocol.dsp.catalog.dispatcher.delegate;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.CATALOG_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogRequestMessageHttpDelegateTest extends DspHttpDispatcherDelegateTestBase<CatalogRequestMessage> {

    private CatalogRequestHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new CatalogRequestHttpDelegate(serializer, mapper, registry);
    }

    @Test
    void getMessageType_returnCatalogRequest() {
        assertThat(delegate.getMessageType()).isEqualTo(CatalogRequestMessage.class);
    }

    @Test
    void buildRequest_returnRequest() throws IOException {
        testBuildRequest_shouldReturnRequest(message(), BASE_PATH + CATALOG_REQUEST);
    }

    @Test
    void buildRequest_serializationFails_throwException() throws IOException {
        testBuildRequest_shouldReturnRequest(message(), BASE_PATH + CATALOG_REQUEST);
    }

    @Test
    void parseResponse_returnCatalog() throws IOException {
        var jsonObject = getJsonObject();
        var catalog = Catalog.Builder.newInstance().build();
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);
        var bytes = "test".getBytes();

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(bytes);
        when(mapper.readValue(bytes, JsonObject.class)).thenReturn(jsonObject);
        when(registry.transform(any(JsonObject.class), eq(Catalog.class))).thenReturn(Result.success(catalog));

        var result = delegate.parseResponse().apply(response);

        assertThat(result).isEqualTo(catalog);
        verify(mapper, times(1)).readValue(bytes, JsonObject.class);
        verify(registry, times(1)).transform(isA(JsonObject.class), eq(Catalog.class));
    }

    @Test
    void parseResponse_transformationFails_throwException() throws IOException {
        var jsonObject = getJsonObject();
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn("test".getBytes());
        when(mapper.readValue(any(byte[].class), eq(JsonObject.class))).thenReturn(jsonObject);
        when(registry.transform(any(JsonObject.class), eq(Catalog.class))).thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> delegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_readingResponseBodyFails_throwException() throws IOException {
        testParseResponse_shouldThrowException_whenReadingResponseBodyFails();
    }

    @Test
    void parseResponse_responseBodyNull_throwException() {
        testParseResponse_shouldThrowException_whenResponseBodyNull();
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

    private JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add("prefix", "http://schema").build())
                .add("prefix:key", "value")
                .build();
    }

    private CatalogRequestMessage message() {
        return CatalogRequestMessage.Builder.newInstance()
                .callbackAddress("http://connector")
                .connectorId("connector-id")
                .protocol("protocol")
                .querySpec(QuerySpec.max())
                .build();
    }
    
    @Override
    protected DspHttpDispatcherDelegate<CatalogRequestMessage, ?> delegate() {
        return delegate;
    }
}
