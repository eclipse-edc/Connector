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
class DestinationSecretTokenTest {

    @Test
    void verifyDeserialize() throws IOException {
        var mapper = new ObjectMapper();

        var writer = new StringWriter();

        mapper.writeValue(writer, new DestinationSecretToken("token"));

        var deserialized = mapper.readValue(writer.toString(), DestinationSecretToken.class);

        assertNotNull(deserialized);
        assertEquals("token", deserialized.getToken());

        writer = new StringWriter();

        mapper.writeValue(writer, new DestinationSecretToken("accessKeyId", "secretAccessKey", "token", 1));

        deserialized = mapper.readValue(writer.toString(), DestinationSecretToken.class);
        assertNotNull(deserialized);
        assertEquals("accessKeyId", deserialized.getAccessKeyId());
        assertEquals("secretAccessKey", deserialized.getSecretAccessKey());
        assertEquals("token", deserialized.getToken());
        assertEquals(1L, deserialized.getExpiration());

    }
}
