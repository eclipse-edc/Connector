package com.microsoft.dagx.transfer.demo.protocols.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.ConnectMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class ConnectMessageTest {

    @Test
    void verifyDeserialization() throws IOException {
        var mapper = new ObjectMapper();

        var dataMessage = ConnectMessage.Builder.newInstance();
        var writer = new StringWriter();
        mapper.writeValue(writer, dataMessage.build());

        var deserialized = mapper.readValue(writer.toString(), ConnectMessage.class);

        assertNotNull(deserialized);
    }

}
