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

package org.eclipse.edc.protocol.dsp.http.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.http.TestFixtures.DSP_TRANSFORMER_CONTEXT_V_MOCK;
import static org.eclipse.edc.protocol.dsp.http.TestFixtures.V_MOCK;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonLdRemoteMessageSerializerImplTest {


    private final TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();

    private final TypeManager typeManager = mock();
    private final ObjectMapper mapper = mock();
    private final RemoteMessage message = mock();
    private JsonLdRemoteMessageSerializerImpl serializer;

    @BeforeEach
    void setUp() {
        var jsonLdService = new TitaniumJsonLd(mock(Monitor.class));
        jsonLdService.registerNamespace("schema", "http://schema/"); //needed for compaction
        when(registry.forContext(DSP_TRANSFORMER_CONTEXT_V_MOCK)).thenReturn(registry);
        serializer = new JsonLdRemoteMessageSerializerImpl(dspTransformerRegistry, typeManager, "test", jsonLdService, dataspaceProfileContextRegistry, "scope");
        when(message.getProtocol()).thenReturn(DATASPACE_PROTOCOL_HTTP);
        when(typeManager.getMapper("test")).thenReturn(mapper);
    }

    @Test
    void serialize_shouldReturnString_whenValidMessage() throws JsonProcessingException {
        var json = messageJson();
        var serialized = "serialized";

        when(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.success(registry));
        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.success(json));
        when(dataspaceProfileContextRegistry.getProtocolVersion(DATASPACE_PROTOCOL_HTTP)).thenReturn(V_MOCK);
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(serialized);

        var result = serializer.serialize(message);

        assertThat(result).isEqualTo(serialized);

        verify(registry, times(1)).transform(message, JsonObject.class);
        verify(mapper, times(1))
                .writeValueAsString(argThat(obj -> ((JsonObject) obj).get("schema:key") != null));
    }

    @Test
    void serialize_shouldThrowException_whenTransformationFails() {
        when(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.success(registry));
        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> serializer.serialize(message))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void serialize_shouldThrowException_whenSerializationFails() throws JsonProcessingException {
        var json = messageJson();

        when(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.success(registry));
        when(dataspaceProfileContextRegistry.getProtocolVersion(DATASPACE_PROTOCOL_HTTP)).thenReturn(V_MOCK);
        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.success(json));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> serializer.serialize(message))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void serialize_shouldThrowException_whenProtocolParseFails() throws JsonProcessingException {
        var json = messageJson();

        when(dspTransformerRegistry.forProtocol(DATASPACE_PROTOCOL_HTTP)).thenReturn(Result.failure("failure"));
        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.success(json));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> serializer.serialize(message))
                .isInstanceOf(EdcException.class);
    }

    private JsonObject messageJson() {
        return Json.createObjectBuilder()
                .add("http://schema/key", "value")
                .build();
    }

}
