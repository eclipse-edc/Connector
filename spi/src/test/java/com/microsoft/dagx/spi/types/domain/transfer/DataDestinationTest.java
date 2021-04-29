package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class DataDestinationTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        DataDestination process = DataDestination.Builder.newInstance().type("test").property("foo", "bar").build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        DataDestination deserialized = mapper.readValue(writer.toString(), DataDestination.class);

        assertNotNull(deserialized);

        assertEquals("test", deserialized.getType());
        assertEquals("bar", deserialized.getProperty("foo"));
    }


}
