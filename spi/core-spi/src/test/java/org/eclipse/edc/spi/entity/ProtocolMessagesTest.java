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

package org.eclipse.edc.spi.entity;

import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolMessagesTest {

    private final ObjectMapper mapper = new JacksonTypeManager().getMapper();

    @Test
    void serdes() throws JacksonException {
        var protocolMessages = new ProtocolMessages();
        protocolMessages.addReceived("received");
        protocolMessages.setLastSent("lastSent");

        var json = mapper.writeValueAsString(protocolMessages);
        var deserialized = mapper.readValue(json, ProtocolMessages.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(protocolMessages);
    }
}
