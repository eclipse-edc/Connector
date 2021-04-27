package com.microsoft.dagx.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEntryTest {

    @Test
    void verifyPolymorphicDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(TestExtension.class);

        DataEntry<DataEntryPropertyLookup> entry = DataEntry.Builder.newInstance().id("id").lookup(new TestExtension()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entry);

        @SuppressWarnings("unchecked") DataEntry<DataEntryPropertyLookup> deserialized = mapper.readValue(writer.toString(), DataEntry.class);

        assertNotNull(deserialized);
        assertTrue(deserialized.getLookup() instanceof TestExtension);
        assertEquals("id", deserialized.getId());
    }

    @JsonTypeName("dagx:testextensions")
    public static class TestExtension extends DataEntryPropertyLookup {

        @Override
        public Map<String, Object> getPropertiesForEntity(String id) {
            return null;
        }
    }
}
