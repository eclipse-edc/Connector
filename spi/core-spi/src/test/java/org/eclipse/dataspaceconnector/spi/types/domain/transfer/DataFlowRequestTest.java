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
package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataFlowRequestTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var request = DataFlowRequest.Builder.newInstance()
                .sourceDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("bar").build())
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .destinationType("test")
                .properties(Map.of("key", "value"))
                .build();
        var serialized = mapper.writeValueAsString(request);
        var deserialized = mapper.readValue(serialized, DataFlowRequest.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getProperties().get("key")).isEqualTo("value");
    }
}
