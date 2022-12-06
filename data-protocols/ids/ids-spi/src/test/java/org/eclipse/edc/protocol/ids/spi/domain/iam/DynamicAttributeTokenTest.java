/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.protocol.ids.spi.domain.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicAttributeTokenTest {
    private ObjectMapper mapper;

    @Test
    void verifySerialization() throws IOException {
        DynamicAttributeToken token = DynamicAttributeToken.Builder.newInstance().relativeId("123").tokenValue("value").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, token);

        DynamicAttributeToken deserialized = mapper.readValue(writer.toString(), DynamicAttributeToken.class);
        assertEquals(token.getTokenValue(), deserialized.getTokenValue());
        assertEquals(token.getId(), deserialized.getId());
        assertEquals(token.getTokenFormat(), deserialized.getTokenFormat());
    }

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }
}
