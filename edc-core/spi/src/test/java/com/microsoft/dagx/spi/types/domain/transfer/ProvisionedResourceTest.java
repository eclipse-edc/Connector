/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProvisionedResourceTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TestProvisionedResource process = TestProvisionedResource.Builder.newInstance().id(UUID.randomUUID().toString()).transferProcessId("123").resourceDefinitionId("1").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        TestProvisionedResource deserialized = mapper.readValue(writer.toString(), TestProvisionedResource.class);

        assertNotNull(deserialized);
    }


}
