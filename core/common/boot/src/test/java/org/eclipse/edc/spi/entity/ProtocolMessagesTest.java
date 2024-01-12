/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolMessagesTest {

    private final ObjectMapper mapper = new TypeManager().getMapper();

    @Test
    void serdes() throws JsonProcessingException {
        var protocolMessages = new ProtocolMessages();
        protocolMessages.addReceived("received");
        protocolMessages.setLastSent("lastSent");

        var json = mapper.writeValueAsString(protocolMessages);
        var deserialized = mapper.readValue(json, ProtocolMessages.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(protocolMessages);
    }
}
