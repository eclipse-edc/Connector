/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class ResourceDefinitionTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TestResourceDefinition process = TestResourceDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        TestResourceDefinition deserialized = mapper.readValue(writer.toString(), TestResourceDefinition.class);

        assertNotNull(deserialized);
    }


}
