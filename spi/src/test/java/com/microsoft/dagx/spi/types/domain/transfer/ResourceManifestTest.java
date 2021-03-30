package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class ResourceManifestTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ResourceManifest process = ResourceManifest.Builder.newInstance().build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        ResourceManifest deserialized = mapper.readValue(writer.toString(), ResourceManifest.class);

        assertNotNull(deserialized);
    }


}
