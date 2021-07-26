/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.domain.iam;

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
