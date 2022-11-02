/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataFlowRequestTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException, MalformedURLException {

        var url = new URL("http://test");
        ObjectMapper mapper = new ObjectMapper();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("bar").build())
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .destinationType("test")
                .callbackAddress(url)
                .properties(Map.of("key", "value"))
                .traceContext(Map.of("key2", "value2"))
                .build();
        var serialized = mapper.writeValueAsString(request);
        var deserialized = mapper.readValue(serialized, DataFlowRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getProperties().get("key")).isEqualTo("value");
        assertThat(deserialized.getTraceContext().get("key2")).isEqualTo("value2");
        assertThat(deserialized.getCallbackAddress()).isEqualTo(url);
    }
}
