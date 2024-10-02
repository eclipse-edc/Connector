/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.http.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class JsonLdResponseBodyDeserializerTest {

    private final ObjectMapper objectMapper = mock();
    private final JsonLd jsonLd = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry = mock();
    private final JsonLdResponseBodyDeserializer<Object> bodyExtractor =
            new JsonLdResponseBodyDeserializer<>(Object.class, objectMapper, jsonLd, dspTransformerRegistry);


    @BeforeEach
    void beforeEach() {
        when(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.success(transformerRegistry));
    }

    @Test
    void shouldTransformBody() throws IOException {
        var compacted = Json.createObjectBuilder().build();
        when(objectMapper.readValue(isA(InputStream.class), isA(Class.class))).thenReturn(compacted);
        var object = new Object();
        var expanded = Json.createObjectBuilder().build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(object));

        var result = bodyExtractor.extractBody(createResponseBody(), DATASPACE_PROTOCOL_HTTP);

        assertThat(result).isSameAs(object);
        verify(transformerRegistry).transform(same(expanded), eq(Object.class));
    }

    @Test
    void shouldThrowException_whenDeserializationDoesNotWork() throws IOException {
        doThrow(IOException.class).when(objectMapper).readValue(isA(InputStream.class), isA(Class.class));

        assertThatThrownBy(() -> bodyExtractor.extractBody(createResponseBody(), DATASPACE_PROTOCOL_HTTP)).isInstanceOf(EdcException.class);
        verifyNoInteractions(jsonLd, transformerRegistry);
    }

    @Test
    void shouldThrowException_whenExpansionFails() throws IOException {
        var compacted = Json.createObjectBuilder().build();
        when(objectMapper.readValue(isA(InputStream.class), isA(Class.class))).thenReturn(compacted);
        when(jsonLd.expand(any())).thenReturn(Result.failure("cannot expand"));

        assertThatThrownBy(() -> bodyExtractor.extractBody(createResponseBody(), DATASPACE_PROTOCOL_HTTP)).isInstanceOf(EdcException.class);
        verify(transformerRegistry, times(0)).transform(any(), any());
    }

    @Test
    void shouldThrowException_whenTransformationFails() throws IOException {
        var compacted = Json.createObjectBuilder().build();
        when(objectMapper.readValue(isA(InputStream.class), isA(Class.class))).thenReturn(compacted);
        var expanded = Json.createObjectBuilder().build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("cannot transform"));

        assertThatThrownBy(() -> bodyExtractor.extractBody(createResponseBody(), DATASPACE_PROTOCOL_HTTP)).isInstanceOf(EdcException.class);
    }

    @NotNull
    private ResponseBody createResponseBody() {
        return ResponseBody.create("{}", MediaType.parse("application/json"));
    }
}
