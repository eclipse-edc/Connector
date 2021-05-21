/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class DataAddressTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("test")
                .keyName("somekey")
                .property("foo", "bar").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, dataAddress);

        DataAddress deserialized = mapper.readValue(writer.toString(), DataAddress.class);

        assertNotNull(deserialized);

        assertEquals("test", deserialized.getType());
        assertEquals("bar", deserialized.getProperty("foo"));
    }

    @Test
    void verifyThrowsException() {

//        assertThatThrownBy(() -> DataAddress.Builder.newInstance()
//                .type("sometype")
//                .property("foo", "bar").build()).isInstanceOf(NullPointerException.class).hasMessageContaining("keyName");

        assertThatThrownBy(() -> DataAddress.Builder.newInstance()
                .keyName("somekey")
                .property("foo", "bar").build()).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }


}
