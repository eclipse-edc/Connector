/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DspDataAddressTest {

    private final ObjectMapper mapper = new JacksonTypeManager().getMapper();

    @Test
    void serdes() throws JsonProcessingException {
        var address = DspDataAddress.Builder.newInstance()
                .endpoint("endpoint")
                .endpointType("endpointType")
                .property("key", "value")
                .build();

        var json = mapper.writeValueAsString(address);

        assertThat(json).contains("\"@type\":\"DataAddress\"").contains("\"@type\":\"EndpointProperty\"");

        var deserialized = mapper.readValue(json, DspDataAddress.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(address);
    }
}
