/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class DataEntryTest {

    @Test
    void verifyPolymorphicDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(TestExtension.class);

        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id("id").catalog(new TestExtension()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entry);

        @SuppressWarnings("unchecked") DataEntry<DataCatalog> deserialized = mapper.readValue(writer.toString(), DataEntry.class);

        assertNotNull(deserialized);
        assertTrue(deserialized.getCatalog() instanceof TestExtension);
        assertEquals("id", deserialized.getId());
    }

    @JsonTypeName("dagx:testextensions")
    public static class TestExtension implements DataCatalog {

        @Override
        public DataAddress getPropertiesForEntity(String id) {
            return null;
        }
    }
}