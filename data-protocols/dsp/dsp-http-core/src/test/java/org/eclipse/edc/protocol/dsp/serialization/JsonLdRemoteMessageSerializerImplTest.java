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

package org.eclipse.edc.protocol.dsp.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonLdRemoteMessageSerializerImplTest {

    private final TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);
    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final RemoteMessage message = mock(RemoteMessage.class);
    private JsonLdRemoteMessageSerializerImpl serializer;
    private TitaniumJsonLd jsonLdService;

    @BeforeEach
    void setUp() {
        jsonLdService = new TitaniumJsonLd(mock(Monitor.class));
        jsonLdService.registerNamespace("schema", "http://schema/"); //needed for compaction
        serializer = new JsonLdRemoteMessageSerializerImpl(registry, mapper, jsonLdService);
    }

    @Test
    void serialize_shouldReturnString_whenValidMessage() throws JsonProcessingException {
        var json = messageJson();
        var serialized = "serialized";

        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.success(json));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(serialized);

        var result = serializer.serialize(message);

        assertThat(result).isEqualTo(serialized);

        verify(registry, times(1)).transform(message, JsonObject.class);
        verify(mapper, times(1))
                .writeValueAsString(argThat(obj -> ((JsonObject) obj).get("schema:key") != null));
    }

    @Test
    void serialize_shouldThrowException_whenTransformationFails() {
        when(registry.transform(message, JsonObject.class))
                .thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> serializer.serialize(message))
                .isInstanceOf(EdcException.class);
    }

    @Test
    void serialize_shouldThrowException_whenSerializationFails() throws JsonProcessingException {
        var json = messageJson();

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
