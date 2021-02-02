package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEntryTest {

    @Test
    void verifyPolymorphicDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(TestExtension.class);

        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().id("id").extensions(new TestExtension()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entry);

        @SuppressWarnings("unchecked") DataEntry<DataEntryExtensions> deserialized = mapper.readValue(writer.toString(), DataEntry.class);

        assertNotNull(deserialized);
        assertTrue(deserialized.getExtensions() instanceof TestExtension);
        assertEquals("id", deserialized.getId());
    }

    @JsonTypeName("dagx:testextensions")
    public static class TestExtension extends DataEntryExtensions {

    }
}
