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
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataFlowRequestTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {

        var uri = URI.create("http://test");
        var mapper = new TypeManager().getMapper();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("bar").build())
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .destinationType("test")
                .callbackAddress(uri)
                .properties(Map.of("key", "value"))
                .traceContext(Map.of("key2", "value2"))
                .build();
        var serialized = mapper.writeValueAsString(request);
        var deserialized = mapper.readValue(serialized, DataFlowRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getProperties().get("key")).isEqualTo("value");
        assertThat(deserialized.getTraceContext().get("key2")).isEqualTo("value2");
        assertThat(deserialized.getCallbackAddress()).isEqualTo(uri);
    }
}
